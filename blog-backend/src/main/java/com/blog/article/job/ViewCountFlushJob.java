package com.blog.article.job;

import com.blog.article.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文章浏览数刷库定时任务：每5分钟全量扫描Redis计数器刷入MySQL
 */
@Slf4j
@Component
public class ViewCountFlushJob {

    @Autowired
    private ArticleService articleService;

    /**
     * 每5分钟执行一次（fixedDelay=300000ms=300秒）
     */
    @Scheduled(fixedDelay = 300000)
    public void flush() {
        try {
            log.info("开始执行文章浏览数刷库任务");
            articleService.flushViewCount();
            log.info("文章浏览数刷库任务执行完成");
        } catch (Exception e) {
            log.error("文章浏览数刷库任务执行失败", e);
        }
    }
}
