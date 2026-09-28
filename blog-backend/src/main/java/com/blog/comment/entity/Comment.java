package com.blog.comment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    private String content;
    /**
     * 顶层评论ID；NULL=直接评论文章
     */
    private Long parentId;
    /**
     * 被回复用户ID；NULL=直接评论文章
     */
    private Long replyToUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
