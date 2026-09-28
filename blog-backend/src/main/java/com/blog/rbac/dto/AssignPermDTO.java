package com.blog.rbac.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色分配权限请求DTO
 */
@Data
public class AssignPermDTO {

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}
