package com.blog.user.controller;

import com.blog.common.result.PageResult;
import com.blog.common.result.Result;
import com.blog.common.security.RequiresPerm;
import com.blog.user.dto.AdminAddUserDTO;
import com.blog.user.dto.AssignRoleDTO;
import com.blog.user.entity.User;
import com.blog.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员用户控制器
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 管理员建号（返回系统生成的随机密码，仅展示一次）
     */
    @PostMapping
    @RequiresPerm("user:add")
    public Result<String> addUser(@Valid @RequestBody AdminAddUserDTO dto) {
        String rawPassword = userService.adminAddUser(dto);
        return Result.ok("创建成功", rawPassword);
    }

    /**
     * 用户列表（分页）
     */
    @GetMapping("/list")
    @RequiresPerm("user:list")
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<User> pageResult = userService.listUsers(page, size);
        return Result.ok(pageResult);
    }

    /**
     * 分配角色
     */
    @PutMapping("/{id}/roles")
    @RequiresPerm("user:role:assign")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRoleDTO dto) {
        List<Long> roleIds = dto == null ? null : dto.getRoleIds();
        userService.assignRoles(id, roleIds);
        return Result.ok();
    }
}
