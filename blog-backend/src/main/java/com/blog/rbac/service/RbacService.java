package com.blog.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.util.RedisUtil;
import com.blog.rbac.dto.MenuVO;
import com.blog.rbac.dto.MyMenuVO;
import com.blog.rbac.dto.RoleDTO;
import com.blog.rbac.entity.Permission;
import com.blog.rbac.entity.Role;
import com.blog.rbac.entity.RolePermission;
import com.blog.rbac.entity.UserRole;
import com.blog.rbac.mapper.PermissionMapper;
import com.blog.rbac.mapper.RoleMapper;
import com.blog.rbac.mapper.RolePermissionMapper;
import com.blog.rbac.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC权限管理服务
 */
@Slf4j
@Service
public class RbacService {

    /**
     * 用户权限码Redis缓存key前缀
     */
    private static final String PERM_CACHE_PREFIX = "user:perms:";

    /**
     * 系统预设角色编码（不可删除）
     */
    private static final Set<String> SYSTEM_ROLE_CODES = Set.of("super_admin", "editor", "comment_admin", "user");

    /**
     * 根节点parentId
     */
    private static final long ROOT_PARENT_ID = 0L;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取当前用户菜单树+权限码
     * 查菜单权限(type=2)+权限码(type=1)，构建菜单树，返回MyMenuVO
     *
     * @param userId 用户ID
     * @return 菜单树+权限码
     */
    public MyMenuVO getMyMenus(Long userId) {
        // 查菜单权限（type=2）
        List<Permission> menus = permissionMapper.selectMenusByUserId(userId);
        // 查权限码（type=1）
        List<String> permCodes = permissionMapper.selectPermCodesByUserId(userId);

        MyMenuVO vo = new MyMenuVO();
        vo.setMenus(buildMenuTree(menus));
        vo.setPerms(permCodes != null ? permCodes : Collections.emptyList());
        return vo;
    }

    /**
     * 全部角色列表
     */
    public List<Role> getRoleList() {
        return roleMapper.selectList(null);
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void addRole(RoleDTO dto) {
        Role role = new Role();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
    }

    /**
     * 编辑角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(BizCodeEnum.BAD_REQUEST, "角色ID不能为空");
        }
        Role role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BizException(BizCodeEnum.ROLE_NOT_FOUND);
        }
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
    }

    /**
     * 删除角色
     * 系统预设角色不可删（role_code in super_admin,editor,comment_admin,user）
     *
     * @param id 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCodeEnum.ROLE_NOT_FOUND);
        }
        if (role.getRoleCode() != null && SYSTEM_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BizException(BizCodeEnum.CANNOT_DELETE_SYSTEM_ROLE);
        }

        // 删除角色与权限的关联
        rolePermissionMapper.deleteByRoleId(id);
        // 删除用户与角色的关联
        QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.eq("role_id", id);
        List<UserRole> userRoles = userRoleMapper.selectList(userRoleWrapper);
        userRoleMapper.delete(userRoleWrapper);

        // 清除受影响用户的权限缓存
        for (UserRole ur : userRoles) {
            clearUserPermCache(ur.getUserId());
        }

        roleMapper.deleteById(id);
    }

    /**
     * 角色绑定权限（先删后增），并清除该角色下所有用户权限缓存
     *
     * @param roleId         角色ID
     * @param permissionIds  权限ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(BizCodeEnum.ROLE_NOT_FOUND);
        }

        // 先删除该角色的所有 role_permission
        rolePermissionMapper.deleteByRoleId(roleId);

        // 再批量插入新的
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }

        // 清除该角色下所有用户的权限缓存
        clearRoleUsersPermCache(roleId);
    }

    /**
     * 权限树（全部权限，按parentId构建树）
     */
    public List<MenuVO> getPermissionTree() {
        List<Permission> all = permissionMapper.selectList(new QueryWrapper<Permission>().orderByAsc("sort"));
        return buildPermissionTree(all);
    }

    /**
     * 查角色已绑权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    public List<Long> getRolePermissions(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 查用户权限码集合（用于JwtInterceptor调用）
     * 优先从Redis缓存读，未命中查库后回写
     *
     * @param userId 用户ID
     * @return 权限码集合
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserPerms(Long userId) {
        String cacheKey = PERM_CACHE_PREFIX + userId;
        Set<String> cached = redisUtil.get(cacheKey, Set.class);
        if (cached != null) {
            return cached;
        }
        // 查库
        List<String> permCodes = permissionMapper.selectPermCodesByUserId(userId);
        Set<String> perms = permCodes != null ? new HashSet<>(permCodes) : new HashSet<>();
        // 回写缓存
        redisUtil.set(cacheKey, perms);
        return perms;
    }

    /**
     * 清除用户权限Redis缓存
     *
     * @param userId 用户ID
     */
    public void clearUserPermCache(Long userId) {
        redisUtil.delete(PERM_CACHE_PREFIX + userId);
    }

    /**
     * 清除该角色下所有用户的权限缓存
     *
     * @param roleId 角色ID
     */
    public void clearRoleUsersPermCache(Long roleId) {
        QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);
        for (UserRole ur : userRoles) {
            clearUserPermCache(ur.getUserId());
        }
    }

    /**
     * 从扁平权限列表构建树形菜单（仅type=2的菜单权限）
     * parentId=0的是根节点，递归找children
     *
     * @param permissions 扁平权限列表
     * @return 菜单树
     */
    private List<MenuVO> buildMenuTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        // 按parentId分组
        Map<Long, List<Permission>> parentMap = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? ROOT_PARENT_ID : p.getParentId()));
        // 构建根节点树
        return buildChildren(parentMap, ROOT_PARENT_ID);
    }

    /**
     * 递归构建子菜单
     */
    private List<MenuVO> buildChildren(Map<Long, List<Permission>> parentMap, Long parentId) {
        List<Permission> children = parentMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        // 按sort排序
        children.sort(Comparator.comparingInt(p -> p.getSort() == null ? 0 : p.getSort()));
        List<MenuVO> result = new ArrayList<>();
        for (Permission p : children) {
            MenuVO vo = toMenuVO(p);
            vo.setChildren(buildChildren(parentMap, p.getId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 从扁平权限列表构建权限树（全部权限，按parentId构建树）
     *
     * @param permissions 扁平权限列表
     * @return 权限树
     */
    private List<MenuVO> buildPermissionTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Permission>> parentMap = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? ROOT_PARENT_ID : p.getParentId()));
        return buildChildren(parentMap, ROOT_PARENT_ID);
    }

    /**
     * Permission转MenuVO
     */
    private MenuVO toMenuVO(Permission p) {
        MenuVO vo = new MenuVO();
        vo.setId(p.getId());
        vo.setPermCode(p.getPermCode());
        vo.setPermName(p.getPermName());
        vo.setPath(p.getPath());
        vo.setIcon(p.getIcon());
        vo.setSort(p.getSort());
        return vo;
    }
}
