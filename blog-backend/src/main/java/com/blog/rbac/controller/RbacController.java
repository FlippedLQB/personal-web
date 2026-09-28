package com.blog.rbac.controller;

import com.blog.common.result.Result;
import com.blog.common.security.RequiresPerm;
import com.blog.common.security.SecurityContext;
import com.blog.rbac.dto.AssignPermDTO;
import com.blog.rbac.dto.MenuVO;
import com.blog.rbac.dto.MyMenuVO;
import com.blog.rbac.dto.RoleDTO;
import com.blog.rbac.entity.Role;
import com.blog.rbac.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC权限管理接口
 */
@RestController
@RequestMapping("/api/rbac")
@Validated
public class RbacController {

    @Autowired
    private RbacService rbacService;

    /**
     * 获取当前用户菜单树+权限码（登录即可访问）
     * blog-admin登录后第一个调用的接口
     */
    @GetMapping("/my/menus")
    public Result<MyMenuVO> myMenus() {
        Long userId = SecurityContext.getCurrentUserId();
        MyMenuVO vo = rbacService.getMyMenus(userId);
        return Result.ok(vo);
    }

    /**
     * 角色列表
     */
    @GetMapping("/role/list")
    @RequiresPerm("role:list")
    public Result<List<Role>> roleList() {
        List<Role> list = rbacService.getRoleList();
        return Result.ok(list);
    }

    /**
     * 新增角色
     */
    @PostMapping("/role")
    @RequiresPerm("role:add")
    public Result<Void> addRole(@Valid @RequestBody RoleDTO dto) {
        rbacService.addRole(dto);
        return Result.ok();
    }

    /**
     * 编辑角色
     */
    @PutMapping("/role")
    @RequiresPerm("role:update")
    public Result<Void> updateRole(@Valid @RequestBody RoleDTO dto) {
        rbacService.updateRole(dto);
        return Result.ok();
    }

    /**
     * 删除角色（系统预设角色不可删）
     *
     * @param id 角色ID
     */
    @DeleteMapping("/role/{id}")
    @RequiresPerm("role:delete")
    public Result<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Result.ok();
    }

    /**
     * 角色分配权限（先删后增）
     *
     * @param id  角色ID
     * @param dto 权限ID列表
     */
    @PutMapping("/role/{id}/permissions")
    @RequiresPerm("role:assignPerm")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody AssignPermDTO dto) {
        rbacService.assignPermissions(id, dto.getPermissionIds());
        return Result.ok();
    }

    /**
     * 查角色已绑权限ID列表
     *
     * @param id 角色ID
     */
    @GetMapping("/role/{id}/permissions")
    @RequiresPerm("role:list")
    public Result<List<Long>> rolePermissions(@PathVariable Long id) {
        List<Long> permIds = rbacService.getRolePermissions(id);
        return Result.ok(permIds);
    }

    /**
     * 权限树（全部权限，按parentId构建树）
     */
    @GetMapping("/permission/tree")
    @RequiresPerm("permission:list")
    public Result<List<MenuVO>> permissionTree() {
        List<MenuVO> tree = rbacService.getPermissionTree();
        return Result.ok(tree);
    }
}
