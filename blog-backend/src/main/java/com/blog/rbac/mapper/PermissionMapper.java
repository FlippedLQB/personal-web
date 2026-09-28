package com.blog.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.rbac.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询权限码列表
     */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> selectByUserId(Long userId);

    /**
     * 根据用户ID查询接口权限码列表
     */
    @Select("SELECT DISTINCT p.perm_code FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.type = 1")
    List<String> selectPermCodesByUserId(Long userId);

    /**
     * 根据用户ID查询菜单权限列表
     */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.type = 2 " +
            "ORDER BY p.sort")
    List<Permission> selectMenusByUserId(Long userId);
}
