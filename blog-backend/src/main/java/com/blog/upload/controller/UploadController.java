package com.blog.upload.controller;

import com.blog.common.result.PageResult;
import com.blog.common.result.Result;
import com.blog.common.security.RequiresPerm;
import com.blog.common.security.SecurityContext;
import com.blog.upload.dto.UploadVO;
import com.blog.upload.entity.ArticleImage;
import com.blog.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传管理接口（管理端）
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 上传图片
     *
     * @param file 图片文件
     */
    @PostMapping("/image")
    @RequiresPerm("image:upload")
    public Result<UploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityContext.getCurrentUserId();
        UploadVO vo = uploadService.uploadImage(file, userId);
        return Result.ok(vo);
    }

    /**
     * 管理端素材列表（含上传者信息）
     *
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     */
    @GetMapping("/list")
    @RequiresPerm("image:list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityContext.getCurrentUserId();
        PageResult<Map<String, Object>> result = uploadService.getAdminImageList(page, size, userId);
        return Result.ok(result);
    }

    /**
     * 删除图片（仅上传者或super_admin可删）
     *
     * @param id 图片ID
     */
    @DeleteMapping("/{id}")
    @RequiresPerm("image:delete")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityContext.getCurrentUserId();
        uploadService.deleteImage(id, userId);
        return Result.ok();
    }
}
