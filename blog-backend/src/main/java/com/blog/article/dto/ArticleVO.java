package com.blog.article.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章展示VO（含作者昵称、浏览数）
 */
@Data
public class ArticleVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 作者用户ID
     */
    private Long userId;

    /**
     * 作者昵称
     */
    private String authorName;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 封图URL
     */
    private String cover;

    /**
     * 状态：0=草稿 1=已发布
     */
    private Integer status;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
