package com.blog.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 管理员建号DTO
 */
@Data
public class AdminAddUserDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 昵称（为空则默认等于用户名）
     */
    private String nickname;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;
}
