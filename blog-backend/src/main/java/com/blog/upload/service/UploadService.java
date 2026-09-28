package com.blog.upload.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.result.PageResult;
import com.blog.common.util.OssUtil;
import com.blog.upload.dto.UploadVO;
import com.blog.upload.entity.ArticleImage;
import com.blog.upload.mapper.ArticleImageMapper;
import com.blog.user.entity.User;
import com.blog.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件上传服务
 */
@Slf4j
@Service
public class UploadService {

    /**
     * 允许的文件后缀白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 最大文件大小：5MB
     */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    @Autowired
    private ArticleImageMapper articleImageMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 上传图片
     * 校验文件(后缀+文件头+大小) → OSS上传 → 入库 → 返回UploadVO
     *
     * @param file   上传的文件
     * @param userId 当前登录用户ID
     * @return 上传结果
     */
    public UploadVO uploadImage(MultipartFile file, Long userId) {
        // 1. 文件校验
        validateFile(file);

        // 2. OSS上传
        String url;
        try (InputStream inputStream = file.getInputStream()) {
            url = ossUtil.upload(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("文件上传读取流失败", e);
            throw new BizException(BizCodeEnum.UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            throw new BizException(BizCodeEnum.UPLOAD_FAILED);
        }

        // 3. 入库记录归属
        ArticleImage image = new ArticleImage();
        image.setUserId(userId);
        image.setUrl(url);
        image.setFileName(file.getOriginalFilename());
        image.setCreateTime(LocalDateTime.now());
        articleImageMapper.insert(image);

        return new UploadVO(url, file.getOriginalFilename());
    }

    /**
     * 公开相册列表（分页返回所有图片）
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页图片列表
     */
    public PageResult<ArticleImage> getImageList(int page, int size) {
        Page<ArticleImage> pageParam = new Page<>(page, size);
        QueryWrapper<ArticleImage> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        Page<ArticleImage> result = articleImageMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /**
     * 管理端素材列表（含上传者信息）
     *
     * @param page   页码
     * @param size   每页条数
     * @param userId 当前登录用户ID（保留用于扩展）
     * @return 分页素材列表，每条记录包含图片信息及上传者昵称
     */
    public PageResult<Map<String, Object>> getAdminImageList(int page, int size, Long userId) {
        Page<ArticleImage> pageParam = new Page<>(page, size);
        QueryWrapper<ArticleImage> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        Page<ArticleImage> result = articleImageMapper.selectPage(pageParam, wrapper);

        List<ArticleImage> images = result.getRecords();
        if (images.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), page, size);
        }

        // 批量查询上传者昵称
        Set<Long> userIds = images.stream()
                .map(ArticleImage::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> uploaderNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                uploaderNameMap.put(user.getId(), user.getNickname());
            }
        }

        // 组装返回结果，包含上传者信息
        List<Map<String, Object>> list = images.stream().map(img -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", img.getId());
            item.put("userId", img.getUserId());
            item.put("url", img.getUrl());
            item.put("fileName", img.getFileName());
            item.put("createTime", img.getCreateTime());
            item.put("uploaderName", uploaderNameMap.getOrDefault(img.getUserId(), ""));
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), page, size);
    }

    /**
     * 删除图片
     * 归属校验（仅上传者或super_admin可删） → OSS删除 → DB删除
     *
     * @param id     图片ID
     * @param userId 当前登录用户ID
     */
    public void deleteImage(Long id, Long userId) {
        ArticleImage image = articleImageMapper.selectById(id);
        if (image == null) {
            throw new BizException(BizCodeEnum.NOT_FOUND, "图片不存在");
        }

        // 归属校验：仅上传者本人或拥有 super_admin 权限可删除
        boolean isOwner = image.getUserId() != null && image.getUserId().equals(userId);
        if (!isOwner) {
            // 通过SecurityContext判断是否super_admin
            Set<String> perms = com.blog.common.security.SecurityContext.getCurrentPerms();
            if (perms == null || !perms.contains("super_admin")) {
                throw new BizException(BizCodeEnum.FORBIDDEN, "无权删除他人上传的图片");
            }
        }

        // OSS删除（失败不阻断流程）
        try {
            ossUtil.delete(image.getUrl());
        } catch (Exception e) {
            log.warn("OSS删除文件失败，url={}, 继续删除DB记录", image.getUrl(), e);
        }

        // DB删除
        articleImageMapper.deleteById(id);
    }

    /**
     * 文件校验：后缀白名单 + 文件头 + 大小限制
     *
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCodeEnum.BAD_REQUEST, "文件不能为空");
        }

        // 1. 大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(BizCodeEnum.FILE_TOO_LARGE);
        }

        // 2. 后缀校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BizException(BizCodeEnum.FILE_TYPE_NOT_ALLOWED);
        }
        String extension = getExtension(originalFilename).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(BizCodeEnum.FILE_TYPE_NOT_ALLOWED);
        }

        // 3. 文件头校验
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 0) {
                throw new BizException(BizCodeEnum.FILE_HEADER_MISMATCH);
            }
            if (!matchFileHeader(extension, header, read)) {
                throw new BizException(BizCodeEnum.FILE_HEADER_MISMATCH);
            }
        } catch (IOException e) {
            log.error("读取文件头失败", e);
            throw new BizException(BizCodeEnum.UPLOAD_FAILED);
        }
    }

    /**
     * 根据扩展名校验文件头是否匹配
     *
     * @param extension 文件扩展名
     * @param header    文件头字节
     * @param len       实际读取长度
     * @return true匹配
     */
    private boolean matchFileHeader(String extension, byte[] header, int len) {
        switch (extension) {
            case "jpg":
            case "jpeg":
                // JPG/JPEG: FF D8 FF (JFIF=FF D8 FF E0, Exif=FF D8 FF E1)
                return len >= 3
                        && (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;
            case "png":
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                return len >= 8
                        && (header[0] & 0xFF) == 0x89
                        && (header[1] & 0xFF) == 0x50
                        && (header[2] & 0xFF) == 0x4E
                        && (header[3] & 0xFF) == 0x47;
            case "gif":
                // GIF: GIF87a 或 GIF89a
                if (len < 6) {
                    return false;
                }
                String gifHeader = new String(header, 0, 6);
                return "GIF87a".equals(gifHeader) || "GIF89a".equals(gifHeader);
            case "webp":
                // WebP: RIFF....WEBP
                if (len < 12) {
                    return false;
                }
                String riff = new String(header, 0, 4);
                String webp = new String(header, 8, 4);
                return "RIFF".equals(riff) && "WEBP".equals(webp);
            default:
                return false;
        }
    }

    /**
     * 获取文件扩展名（不含点号）
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
