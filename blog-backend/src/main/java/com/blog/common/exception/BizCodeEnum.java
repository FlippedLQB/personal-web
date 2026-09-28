package com.blog.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizCodeEnum {
    // 通用
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "操作过于频繁，请稍后再试"),
    LOCKED(423, "账号已被锁定"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USERNAME_EXISTS(1001, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(1002, "用户名或密码错误"),
    ACCOUNT_LOCKED(1003, "账号已被临时锁定，请稍后再试"),
    OLD_PASSWORD_ERROR(1004, "原密码错误"),

    // 文章模块 2xxx
    ARTICLE_NOT_FOUND(2001, "文章不存在"),
    ARTICLE_NOT_PUBLISHED(2002, "文章未发布"),

    // 评论模块 3xxx
    COMMENT_NOT_FOUND(3001, "评论不存在"),
    COMMENT_HAS_REPLIES(3002, "该评论下有回复，无法直接删除"),
    SENSITIVE_CONTENT(3003, "评论包含敏感词，请修改"),

    // 上传模块 4xxx
    FILE_TYPE_NOT_ALLOWED(4001, "文件类型不允许"),
    FILE_TOO_LARGE(4002, "文件大小超过限制"),
    FILE_HEADER_MISMATCH(4003, "文件内容与扩展名不匹配"),
    UPLOAD_FAILED(4004, "上传失败"),

    // RBAC模块 5xxx
    ROLE_NOT_FOUND(5001, "角色不存在"),
    PERMISSION_NOT_FOUND(5002, "权限不存在"),
    CANNOT_DELETE_SYSTEM_ROLE(5003, "系统预设角色不可删除");

    private final int code;
    private final String message;
}
