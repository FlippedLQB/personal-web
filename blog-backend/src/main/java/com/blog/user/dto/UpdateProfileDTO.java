package com.blog.user.dto;

import lombok.Data;

/**
 * 修改个人资料DTO
 * 若oldPassword和newPassword同时不为空，则校验原密码后修改密码
 */
@Data
public class UpdateProfileDTO {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 原密码（修改密码时必填）
     */
    private String oldPassword;

    /**
     * 新密码（修改密码时必填）
     */
    private String newPassword;
}
