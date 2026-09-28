package com.blog.article.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.article.dto.ArticleDTO;
import com.blog.article.dto.ArticleQueryDTO;
import com.blog.article.dto.ArticleVO;
import com.blog.article.entity.Article;
import com.blog.article.mapper.ArticleMapper;
import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.result.PageResult;
import com.blog.common.security.SecurityContext;
import com.blog.common.util.RedisUtil;
import com.blog.user.entity.User;
import com.blog.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章服务：文章CRUD、浏览计数（IP去重+Redis计数+阈值/定时刷库）、详情缓存
 */
@Slf4j
@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtil redisUtil;

    /** 文章详情缓存key前缀 article:detail:{id}，TTL 600秒 */
    private static final String CACHE_DETAIL_PREFIX = "article:detail:";
    private static final int CACHE_DETAIL_TTL = 600;

    /** 浏览计数Redis key前缀 article:view:{id}（无TTL，定时刷库清零） */
    private static final String VIEW_COUNT_PREFIX = "article:view:";

    /** 浏览去重key前缀 article:view:dedup:{id}:{ip}，TTL 300秒 */
    private static final String VIEW_DEDUP_PREFIX = "article:view:dedup:";
    private static final int VIEW_DEDUP_TTL = 300;

    /** 同IP同文章去重时间窗口内不再计入浏览 */
    /** 阈值触发刷库：累计增量达50立即刷库 */
    private static final long FLUSH_THRESHOLD = 50L;
    /** 计数器TTL：设较大值近似"无TTL"，避免孤儿key泄漏；刷库任务会主动清零 */
    private static final int COUNTER_TTL = 30 * 24 * 3600;

    /**
     * 公开分页查询已发布文章
     */
    public PageResult<ArticleVO> getPublicList(ArticleQueryDTO query) {
        Page<Article> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
                .like(StringUtils.hasText(query.getKeyword()), Article::getTitle, query.getKeyword())
                .orderByDesc(Article::getCreateTime);
        articleMapper.selectPage(page, wrapper);
        return PageResult.of(convertToVOList(page.getRecords()), page.getTotal(), query.getPage(), query.getSize());
    }

    /**
     * 管理端分页查询全部文章（含草稿），需 article:list 权限
     */
    public PageResult<ArticleVO> getAdminList(ArticleQueryDTO query) {
        Page<Article> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStatus() != null, Article::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Article::getTitle, query.getKeyword())
                .orderByDesc(Article::getCreateTime);
        articleMapper.selectPage(page, wrapper);
        return PageResult.of(convertToVOList(page.getRecords()), page.getTotal(), query.getPage(), query.getSize());
    }

    /**
     * 公开获取文章详情，含浏览计数（IP去重 + Redis INCR + 阈值/定时刷库）
     */
    public ArticleVO getArticleDetail(Long id, String ip) {
        ArticleVO vo = getCachedArticle(id);
        if (vo == null) {
            Article article = articleMapper.selectById(id);
            if (article == null) {
                throw new BizException(BizCodeEnum.ARTICLE_NOT_FOUND);
            }
            if (article.getStatus() == null || article.getStatus() != 1) {
                throw new BizException(BizCodeEnum.ARTICLE_NOT_PUBLISHED);
            }
            vo = buildVOWithAuthor(article);
            cacheArticle(vo);
        }
        // 累加Redis待刷库增量，展示最新浏览数
        long pending = incrementViewCount(id, ip);
        vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + (int) pending);
        return vo;
    }

    /**
     * 管理端获取文章详情（不计浏览数）
     */
    public ArticleVO getArticleDetailAdmin(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(BizCodeEnum.ARTICLE_NOT_FOUND);
        }
        return buildVOWithAuthor(article);
    }

    /**
     * 创建文章
     */
    public Long createArticle(ArticleDTO dto, Long userId) {
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setUserId(userId);
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        article.setViewCount(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
        return article.getId();
    }

    /**
     * 更新文章，需归属校验
     */
    public void updateArticle(ArticleDTO dto, Long userId) {
        if (dto.getId() == null) {
            throw new BizException(BizCodeEnum.BAD_REQUEST, "文章ID不能为空");
        }
        Article existing = articleMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException(BizCodeEnum.ARTICLE_NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.FORBIDDEN, "无权修改他人文章");
        }
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        evictCache(dto.getId());
    }

    /**
     * 删除文章，需归属校验
     * 拥有 article:delete:any 权限跳过归属校验（super_admin）
     */
    public void deleteArticle(Long id, Long userId) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(BizCodeEnum.ARTICLE_NOT_FOUND);
        }
        if (!SecurityContext.hasPerm("article:delete:any")
                && !existing.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.FORBIDDEN, "无权删除他人文章");
        }
        articleMapper.deleteById(id);
        evictCache(id);
        redisUtil.delete(VIEW_COUNT_PREFIX + id);
    }

    /**
     * 定时任务调用：全量扫描Redis浏览计数刷库
     */
    public void flushViewCount() {
        Set<String> keys = redisUtil.keys(VIEW_COUNT_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            // 跳过去重key（article:view:dedup:...）
            if (key.contains(":dedup:")) {
                continue;
            }
            String idStr = key.substring(VIEW_COUNT_PREFIX.length());
            try {
                Long articleId = Long.parseLong(idStr);
                Long pending = redisUtil.getAndDelete(key);
                if (pending != null && pending > 0) {
                    articleMapper.addViewCount(articleId, pending.intValue());
                    // 刷新详情缓存，保证浏览数展示准确
                    evictCache(articleId);
                }
            } catch (NumberFormatException e) {
                log.warn("刷库时无法解析文章ID, key={}", key);
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 浏览计数：IP去重 + Redis INCR，阈值触发立即刷库
     *
     * @return 当前待刷库增量
     */
    private long incrementViewCount(Long articleId, String ip) {
        String dedupKey = VIEW_DEDUP_PREFIX + articleId + ":" + (ip == null ? "unknown" : ip);
        // 5分钟内同IP同文章只计1次
        if (redisUtil.exists(dedupKey)) {
            return readCounter(VIEW_COUNT_PREFIX + articleId);
        }
        redisUtil.set(dedupKey, "1", VIEW_DEDUP_TTL);
        long count = redisUtil.increment(VIEW_COUNT_PREFIX + articleId, 1, COUNTER_TTL);
        // 阈值触发：累计达50立即刷库并清零
        if (count >= FLUSH_THRESHOLD) {
            Long pending = redisUtil.getAndDelete(VIEW_COUNT_PREFIX + articleId);
            if (pending != null && pending > 0) {
                articleMapper.addViewCount(articleId, pending.intValue());
                evictCache(articleId);
            }
            return 0;
        }
        return count;
    }

    private long readCounter(String key) {
        String val = redisUtil.get(key);
        if (val == null) {
            return 0;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void cacheArticle(ArticleVO vo) {
        try {
            redisUtil.set(CACHE_DETAIL_PREFIX + vo.getId(), vo, CACHE_DETAIL_TTL);
        } catch (Exception e) {
            log.warn("缓存文章详情失败, id={}", vo.getId(), e);
        }
    }

    private ArticleVO getCachedArticle(Long id) {
        try {
            return redisUtil.get(CACHE_DETAIL_PREFIX + id, ArticleVO.class);
        } catch (Exception e) {
            log.warn("读取文章详情缓存失败, id={}", id, e);
            return null;
        }
    }

    private void evictCache(Long id) {
        redisUtil.delete(CACHE_DETAIL_PREFIX + id);
    }

    private ArticleVO buildVOWithAuthor(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        if (article.getUserId() != null) {
            User author = userMapper.selectById(article.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
            }
        }
        return vo;
    }

    private List<ArticleVO> convertToVOList(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询作者信息
        Set<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return articles.stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(a, vo);
            User u = userMap.get(a.getUserId());
            if (u != null) {
                vo.setAuthorName(u.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
