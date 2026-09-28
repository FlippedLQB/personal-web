package com.blog.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.comment.dto.CommentDTO;
import com.blog.comment.dto.CommentVO;
import com.blog.comment.entity.Comment;
import com.blog.comment.mapper.CommentMapper;
import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.result.PageResult;
import com.blog.common.sensitive.SensitiveFilter;
import com.blog.common.security.SecurityContext;
import com.blog.user.entity.User;
import com.blog.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务：两层评论查询、敏感词校验、级联删除
 */
@Slf4j
@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    /**
     * 分页查询评论（两层查询：先分页查顶层评论，再IN查回复）
     */
    public PageResult<CommentVO> listByArticleId(Long articleId, Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;

        // 第一层：分页查顶层评论（parent_id IS NULL）
        Page<Comment> pager = new Page<>(p, s);
        LambdaQueryWrapper<Comment> topWrapper = new LambdaQueryWrapper<>();
        topWrapper.eq(Comment::getArticleId, articleId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreateTime);
        commentMapper.selectPage(pager, topWrapper);
        List<Comment> topComments = pager.getRecords();

        if (topComments.isEmpty()) {
            return PageResult.of(Collections.emptyList(), pager.getTotal(), p, s);
        }

        // 第二层：IN查所有回复
        List<Long> topIds = topComments.stream().map(Comment::getId).collect(Collectors.toList());
        LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(Comment::getArticleId, articleId)
                .in(Comment::getParentId, topIds)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> replies = commentMapper.selectList(replyWrapper);

        // 按顶层评论ID分组回复
        Map<Long, List<Comment>> repliesMap = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 批量查询用户信息（评论者 + 被回复者）
        Set<Long> userIds = new HashSet<>();
        for (Comment c : topComments) {
            if (c.getUserId() != null) userIds.add(c.getUserId());
        }
        for (Comment r : replies) {
            if (r.getUserId() != null) userIds.add(r.getUserId());
            if (r.getReplyToUserId() != null) userIds.add(r.getReplyToUserId());
        }
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 组装VO：顶层评论 + 嵌套回复
        List<CommentVO> voList = topComments.stream().map(c -> {
            CommentVO vo = toVO(c, userMap);
            List<CommentVO> replyVOs = repliesMap.getOrDefault(c.getId(), Collections.emptyList())
                    .stream().map(r -> toVO(r, userMap)).collect(Collectors.toList());
            vo.setReplies(replyVOs);
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, pager.getTotal(), p, s);
    }

    /**
     * 新增评论/回复，敏感词校验
     */
    public Long addComment(CommentDTO dto, Long userId) {
        // 敏感词校验
        if (sensitiveFilter.containsSensitive(dto.getContent())) {
            throw new BizException(BizCodeEnum.SENSITIVE_CONTENT);
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(userId);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        if (dto.getParentId() != null) {
            // 回复：校验顶层评论存在
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BizException(BizCodeEnum.COMMENT_NOT_FOUND);
            }
            // parent_id 始终指向顶层评论（永不三层）
            if (parent.getParentId() != null) {
                comment.setParentId(parent.getParentId());
            }
        }
        commentMapper.insert(comment);
        return comment.getId();
    }

    /**
     * 删除评论，有 comment:delete:any 跳过归属，否则归属校验；顶层评论级联删回复
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id, Long userId) {
        Comment existing = commentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_FOUND);
        }
        if (!SecurityContext.hasPerm("comment:delete:any")
                && !existing.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.FORBIDDEN, "无权删除他人评论");
        }
        // 顶层评论级联删除其下所有回复
        if (existing.getParentId() == null) {
            commentMapper.deleteRepliesByParentId(id);
        }
        commentMapper.deleteById(id);
    }

    // ==================== 私有方法 ====================

    private CommentVO toVO(Comment c, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(c, vo);
        User u = userMap.get(c.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
        }
        if (c.getReplyToUserId() != null) {
            User ru = userMap.get(c.getReplyToUserId());
            if (ru != null) {
                vo.setReplyToNickname(ru.getNickname());
            }
        }
        // 回复的replies固定为空列表
        if (vo.getReplies() == null) {
            vo.setReplies(Collections.emptyList());
        }
        return vo;
    }
}
