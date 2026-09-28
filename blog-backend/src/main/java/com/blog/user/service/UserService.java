package com.blog.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.jwt.JwtProperties;
import com.blog.common.jwt.JwtUtil;
import com.blog.common.ratelimit.RateLimit;
import com.blog.common.result.PageResult;
import com.blog.common.util.RedisUtil;
import com.blog.rbac.entity.UserRole;
import com.blog.rbac.mapper.PermissionMapper;
import com.blog.rbac.mapper.RoleMapper;
import com.blog.rbac.mapper.UserRoleMapper;
import com.blog.user.dto.*;
import com.blog.user.entity.User;
import com.blog.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * BCrypt密码编码器（无状态，可直接实例化）
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 普通用户角色ID */
    private static final long DEFAULT_USER_ROLE_ID = 4L;

    /** 登录失败最大次数 */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 账号锁定时长（秒） */
    private static final long ACCOUNT_LOCK_SECONDS = 600L;

    /** Redis Key 前缀 */
    private static final String TOKEN_KEY_PREFIX = "user:token:";
    private static final String PERMS_KEY_PREFIX = "user:perms:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    /**
     * 注册：校验用户名唯一 → BCrypt加密 → 入库 → 绑定user角色
     * 限流：5次/IP/天
     */
    @RateLimit(key = "register", limit = 5, window = 86400)
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // 校验用户名唯一
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (existCount != null && existCount > 0) {
            throw new BizException(BizCodeEnum.USERNAME_EXISTS);
        }

        // 构建用户实体，昵称默认等于用户名
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        userMapper.insert(user);

        // 绑定普通用户角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(DEFAULT_USER_ROLE_ID);
        userRoleMapper.insert(userRole);

        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    /**
     * 登录：校验账号锁定 → 校验账号密码 → 成功清失败计数/失败增计数 → 生成JWT → 存Redis
     * 限流：10次/IP/分钟
     */
    @RateLimit(key = "login", limit = 10, window = 60)
    public LoginVO login(LoginDTO dto) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + dto.getUsername();

        // 检查账号是否被锁定
        String failStr = redisUtil.get(failKey);
        long failCount = failStr == null ? 0 : Long.parseLong(failStr);
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            long retryAfter = redisUtil.getExpire(failKey);
            if (retryAfter < 1) {
                retryAfter = ACCOUNT_LOCK_SECONDS;
            }
            int remainingMinutes = (int) Math.ceil(retryAfter / 60.0);
            throw new BizException(423,
                    "账号已被临时锁定，请" + remainingMinutes + "分钟后再试");
        }

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));

        // 校验密码
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // 失败：增加失败计数，达到5次则锁定10分钟
            long newCount = redisUtil.increment(failKey, 1, (int) ACCOUNT_LOCK_SECONDS);
            if (newCount >= MAX_LOGIN_FAIL_COUNT) {
                // 第5次失败时重置TTL为10分钟
                redisUtil.expire(failKey, ACCOUNT_LOCK_SECONDS);
            }
            throw new BizException(BizCodeEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // 成功：清除失败计数
        redisUtil.delete(failKey);

        // 生成JWT并存入Redis，支持主动踢下线
        String token = jwtUtil.generateToken(user.getId());
        long tokenTtlSeconds = jwtProperties.getExpiration() / 1000;
        redisUtil.set(TOKEN_KEY_PREFIX + user.getId(), token, tokenTtlSeconds);

        // 将权限集合存入Redis，供JwtInterceptor读取
        List<String> permCodes = permissionMapper.selectPermCodesByUserId(user.getId());
        Set<String> perms = permCodes == null ? new HashSet<>() : new HashSet<>(permCodes);
        redisUtil.set(PERMS_KEY_PREFIX + user.getId(), perms);

        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    /**
     * 获取当前登录用户信息（基本信息 + 角色列表）
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.NOT_FOUND);
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoles(roleMapper.selectByUserId(userId));
        return vo;
    }

    /**
     * 修改个人资料：更新昵称/头像；若含oldPassword+newPassword则验证原密码后更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.NOT_FOUND);
        }

        boolean needUpdate = false;

        // 更新昵称
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
            needUpdate = true;
        }
        // 更新头像
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
            needUpdate = true;
        }
        // 修改密码：oldPassword和newPassword同时提供时才执行
        if (StringUtils.hasText(dto.getOldPassword()) && StringUtils.hasText(dto.getNewPassword())) {
            // 校验原密码
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BizException(BizCodeEnum.OLD_PASSWORD_ERROR);
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            needUpdate = true;
        }

        if (needUpdate) {
            userMapper.updateById(user);
            log.info("用户资料更新成功: id={}", userId);
        }
    }

    /**
     * 管理员建号：生成随机密码 → BCrypt加密 → 入库 → 绑定角色 → 返回明文密码（展示一次）
     */
    @Transactional(rollbackFor = Exception.class)
    public String adminAddUser(AdminAddUserDTO dto) {
        // 校验用户名唯一
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (existCount != null && existCount > 0) {
            throw new BizException(BizCodeEnum.USERNAME_EXISTS);
        }

        // 生成随机密码
        String rawPassword = generateRandomPassword();

        // 构建用户实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        // 昵称为空则默认等于用户名
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        userMapper.insert(user);

        // 绑定角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        log.info("管理员建号成功: id={}, username={}", user.getId(), user.getUsername());
        // 返回明文密码，仅展示给管理员一次
        return rawPassword;
    }

    /**
     * 分配角色：删除旧角色 → 绑定新角色 → 刷新权限缓存
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除旧的用户角色关系
        userRoleMapper.deleteByUserId(userId);

        // 绑定新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        // 刷新Redis中的权限缓存，使新角色权限立即生效
        List<String> permCodes = permissionMapper.selectPermCodesByUserId(userId);
        Set<String> perms = permCodes == null ? new HashSet<>() : new HashSet<>(permCodes);
        redisUtil.set(PERMS_KEY_PREFIX + userId, perms);

        log.info("角色分配成功: userId={}, roleIds={}", userId, roleIds);
    }

    /**
     * 分页查询用户列表
     */
    public PageResult<User> listUsers(int page, int size) {
        Page<User> pageObj = new Page<>(page, size);
        userMapper.selectPage(pageObj, null);
        return PageResult.of(pageObj.getRecords(), pageObj.getTotal(), page, size);
    }

    /**
     * 生成8位随机字母数字密码
     */
    public String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
