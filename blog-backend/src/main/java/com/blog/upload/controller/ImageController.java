package com.blog.upload.controller;

import com.blog.common.result.PageResult;
import com.blog.common.result.Result;
import com.blog.upload.entity.ArticleImage;
import com.blog.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开相册接口（无需登录）
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private UploadService uploadService;

    /**
     * 公开相册列表（分页返回所有图片）
     *
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     */
    @GetMapping("/list")
    public Result<PageResult<ArticleImage>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<ArticleImage> result = uploadService.getImageList(page, size);
        return Result.ok(result);
    }
}
