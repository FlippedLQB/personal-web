package com.blog.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论展示VO（两层结构：顶层评论 + 嵌套回复列表）
 */
@Data
public class CommentVO {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论者用户ID
     */
    private Long userId;

    /**
     * 评论者昵称
     */
    private String nickname;

    /**
     * 评论者头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 顶层评论ID（NULL=本条即为顶层评论）
     */
    private Long parentId;

    /**
     * 被回复用户ID（NULL=直接评论文章）
     */
    private Long replyToUserId;

    /**
     * 被回复者昵称（仅回复时有值）
     */
    private String replyToNickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 嵌套回复列表（仅顶层评论有值；回复固定为空列表）
     */
    private List<CommentVO> replies;
}
