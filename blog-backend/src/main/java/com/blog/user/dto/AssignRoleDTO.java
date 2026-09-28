package com.blog.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 分配角色DTO
 */
@Data
public class AssignRoleDTO {

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;
}
