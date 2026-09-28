package com.blog.article.dto;

import lombok.Data;

/**
 * 文章分页查询条件DTO
 */
@Data
public class ArticleQueryDTO {

    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页条数，默认10
     */
    private Integer size = 10;

    /**
     * 状态过滤：0=草稿 1=已发布，null=不过滤
     */
    private Integer status;

    /**
     * 关键词（按标题模糊搜索）
     */
    private String keyword;
}
