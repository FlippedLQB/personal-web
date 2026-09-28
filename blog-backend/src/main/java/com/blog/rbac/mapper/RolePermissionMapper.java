package com.blog.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.rbac.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(Long roleId);

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(Long roleId);

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int delete(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
