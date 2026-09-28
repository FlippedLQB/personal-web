package com.blog.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章创建/更新请求DTO
 * id：更新时必传，创建时为空（由Service层校验）
 */
@Data
public class ArticleDTO {

    /**
     * 文章ID（更新时必传）
     */
    private Long id;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 正文内容
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
}
