package com.blog.user.controller;

import com.blog.common.result.Result;
import com.blog.common.security.SecurityContext;
import com.blog.user.dto.UpdateProfileDTO;
import com.blog.user.dto.UserInfoVO;
import com.blog.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人资料控制器（登录即可访问，无特殊权限码）
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户信息（基本信息 + 角色列表）
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        Long userId = SecurityContext.getCurrentUserId();
        UserInfoVO vo = userService.getUserInfo(userId);
        return Result.ok(vo);
    }

    /**
     * 修改个人资料（昵称、头像、密码）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        userService.updateProfile(userId, dto);
        return Result.ok();
    }
}
