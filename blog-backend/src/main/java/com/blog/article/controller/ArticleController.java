package com.blog.article.controller;

import com.blog.article.dto.ArticleDTO;
import com.blog.article.dto.ArticleQueryDTO;
import com.blog.article.dto.ArticleVO;
import com.blog.article.service.ArticleService;
import com.blog.common.result.PageResult;
import com.blog.common.result.Result;
import com.blog.common.security.RequiresPerm;
import com.blog.common.security.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文章Controller，路径 /api/article
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * 公开文章列表（无需登录）
     */
    @GetMapping("/list")
    public Result<PageResult<ArticleVO>> publicList(ArticleQueryDTO query) {
        return Result.ok(articleService.getPublicList(query));
    }

    /**
     * 公开文章详情（无需登录），含浏览计数
     */
    @GetMapping("/detail/{id}")
    public Result<ArticleVO> publicDetail(@PathVariable Long id, HttpServletRequest request) {
        String ip = getClientIp(request);
        return Result.ok(articleService.getArticleDetail(id, ip));
    }

    /**
     * 管理端文章列表（含草稿）
     */
    @RequiresPerm("article:list")
    @GetMapping("/admin/list")
    public Result<PageResult<ArticleVO>> adminList(ArticleQueryDTO query) {
        return Result.ok(articleService.getAdminList(query));
    }

    /**
     * 管理端文章详情（不计浏览数）
     */
    @RequiresPerm("article:list")
    @GetMapping("/admin/{id}")
    public Result<ArticleVO> adminDetail(@PathVariable Long id) {
        return Result.ok(articleService.getArticleDetailAdmin(id));
    }

    /**
     * 创建文章
     */
    @RequiresPerm("article:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid ArticleDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        return Result.ok(articleService.createArticle(dto, userId));
    }

    /**
     * 更新文章
     */
    @RequiresPerm("article:update")
    @PutMapping
    public Result<Void> update(@RequestBody @Valid ArticleDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        articleService.updateArticle(dto, userId);
        return Result.ok();
    }

    /**
     * 删除文章
     */
    @RequiresPerm("article:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityContext.getCurrentUserId();
        articleService.deleteArticle(id, userId);
        return Result.ok();
    }

    /**
     * 从请求头获取客户端IP：X-Forwarded-For / X-Real-IP / RemoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
