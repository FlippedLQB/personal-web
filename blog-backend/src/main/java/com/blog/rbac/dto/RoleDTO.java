package com.blog.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色创建/编辑请求DTO
 * id：编辑时必传，创建时为空
 */
@Data
public class RoleDTO {

    /**
     * 角色ID（编辑时必传）
     */
    private Long id;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 角色描述
     */
    private String description;
}
