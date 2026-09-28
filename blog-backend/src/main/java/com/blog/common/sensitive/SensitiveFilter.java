package com.blog.common.sensitive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器
 * 使用简单的敏感词集合匹配，实际项目中可替换为DFA算法
 */
@Slf4j
@Component
public class SensitiveFilter {

    // 默认敏感词集合，实际项目可从数据库或配置文件加载
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "赌博", "色情", "暴力", "毒品", "枪支", "炸药", "反动"
    ));

    // 敏感词替换符
    private static final String REPLACEMENT = "***";

    /**
     * 检查文本是否包含敏感词
     */
    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                log.warn("检测到敏感词: {}", word);
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤敏感词，替换为 ***
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + Pattern.quote(word), REPLACEMENT);
        }
        return result;
    }
}
