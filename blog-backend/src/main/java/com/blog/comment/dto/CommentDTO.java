package com.blog.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论新增/回复请求DTO
 */
@Data
public class CommentDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /**
     * 评论内容（长度1-1000）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论内容长度必须在1-1000之间")
    private String content;

    /**
     * 顶层评论ID（回复时必传；NULL=直接评论文章）
     */
    private Long parentId;

    /**
     * 被回复用户ID（回复他人时传；NULL=回复顶层评论作者）
     */
    private Long replyToUserId;
}
