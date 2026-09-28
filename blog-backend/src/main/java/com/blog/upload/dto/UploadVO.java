package com.blog.upload.dto;

import lombok.Data;

/**
 * 文件上传结果VO
 */
@Data
public class UploadVO {

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 原始文件名
     */
    private String fileName;

    public UploadVO() {
    }

    public UploadVO(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
    }
}
