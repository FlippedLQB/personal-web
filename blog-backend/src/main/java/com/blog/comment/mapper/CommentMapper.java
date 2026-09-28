package com.blog.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 级联删除顶层评论下的所有回复
     */
    @Delete("DELETE FROM comment WHERE parent_id = #{parentId}")
    int deleteRepliesByParentId(@Param("parentId") Long parentId);
}
