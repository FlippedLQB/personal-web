package com.blog.upload.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_image")
public class ArticleImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String url;
    private String fileName;
    private LocalDateTime createTime;
}
