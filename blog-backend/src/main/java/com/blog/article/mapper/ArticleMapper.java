package com.blog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.article.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 增量更新浏览数
     */
    @Update("UPDATE article SET view_count = view_count + #{delta} WHERE id = #{articleId}")
    int addViewCount(@Param("articleId") Long articleId, @Param("delta") int delta);
}
