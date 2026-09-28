package com.blog.common.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class OssUtil {

    @Value("${blog.oss.endpoint}")
    private String endpoint;

    @Value("${blog.oss.access-key-id}")
    private String accessKeyId;

    @Value("${blog.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${blog.oss.bucket-name}")
    private String bucketName;

    @Value("${blog.oss.domain}")
    private String domain;

    private OSS createClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 上传文件，返回完整URL
     */
    public String upload(InputStream inputStream, String originalFileName) {
        String objectKey = buildObjectKey(originalFileName);
        OSS ossClient = createClient();
        try {
            PutObjectResult result = ossClient.putObject(bucketName, objectKey, inputStream);
            log.info("OSS上传成功: {}", objectKey);
            return domain + "/" + objectKey;
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 删除文件
     */
    public void delete(String fileUrl) {
        String objectKey = fileUrl.replace(domain + "/", "");
        OSS ossClient = createClient();
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("OSS删除成功: {}", objectKey);
        } catch (Exception e) {
            log.error("OSS删除失败", e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 生成存储路径：yyyy/MM/dd/uuid.ext
     */
    private String buildObjectKey(String originalFileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return datePath + "/" + uuid + ext;
    }
}
