package com.blog.comment.controller;

import com.blog.comment.dto.CommentDTO;
import com.blog.comment.dto.CommentVO;
import com.blog.comment.service.CommentService;
import com.blog.common.ratelimit.RateLimit;
import com.blog.common.result.PageResult;
import com.blog.common.result.Result;
import com.blog.common.security.RequiresPerm;
import com.blog.common.security.SecurityContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评论Controller，路径 /api/comment
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 公开评论列表（分页参数page/size）
     */
    @GetMapping("/list/{articleId}")
    public Result<PageResult<CommentVO>> list(@PathVariable Long articleId,
                                              @RequestParam(value = "page", defaultValue = "1") Integer page,
                                              @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.ok(commentService.listByArticleId(articleId, page, size));
    }

    /**
     * 新增评论/回复，限流20次/IP/天
     */
    @RateLimit(key = "comment", limit = 20, window = 86400)
    @RequiresPerm("comment:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CommentDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        return Result.ok(commentService.addComment(dto, userId));
    }

    /**
     * 删除评论，拥有 comment:delete 或 comment:delete:any 任一权限即可
     */
    @RequiresPerm(anyOf = {"comment:delete", "comment:delete:any"})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityContext.getCurrentUserId();
        commentService.deleteComment(id, userId);
        return Result.ok();
    }
}
