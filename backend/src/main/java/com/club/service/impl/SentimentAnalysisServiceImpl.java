package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.dto.SentimentAnalysisResult;
import com.club.entity.SentimentDictionary;
import com.club.mapper.SentimentDictionaryMapper;
import com.club.service.SentimentAnalysisService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SentimentAnalysisServiceImpl implements SentimentAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalysisServiceImpl.class);

    @Autowired
    private SentimentDictionaryMapper dictionaryMapper;

    private List<SentimentDictionary> positiveWords = new ArrayList<>();
    private List<SentimentDictionary> negativeWords = new ArrayList<>();

    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "[\\x{1F300}-\\x{1F6FF}" +
        "\\x{1F900}-\\x{1F9FF}" +
        "\\x{2600}-\\x{26FF}" +
        "\\x{2700}-\\x{27BF}" +
        "\\x{1F680}-\\x{1F6C0}" +
        "\\x{1F600}-\\x{1F64F}]",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile(
        "[\\p{P}\\p{S}\\s]",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    @PostConstruct
    public void init() {
        reloadDictionary();
    }

    @Override
    public void reloadDictionary() {
        logger.info("开始加载情绪分析词典...");
        List<SentimentDictionary> allWords = dictionaryMapper.selectList(
            new LambdaQueryWrapper<SentimentDictionary>()
                .eq(SentimentDictionary::getIsActive, 1)
        );

        positiveWords.clear();
        negativeWords.clear();

        for (SentimentDictionary word : allWords) {
            if ("POSITIVE".equals(word.getSentiment())) {
                positiveWords.add(word);
            } else if ("NEGATIVE".equals(word.getSentiment())) {
                negativeWords.add(word);
            }
        }

        positiveWords.sort((a, b) -> b.getKeyword().length() - a.getKeyword().length());
        negativeWords.sort((a, b) -> b.getKeyword().length() - a.getKeyword().length());

        logger.info("情绪分析词典加载完成，正面关键词：{} 个，负面关键词：{} 个",
            positiveWords.size(), negativeWords.size());
    }

    @Override
    public SentimentAnalysisResult analyze(String text) {
        List<String> matchedTags = new ArrayList<>();
        int positiveScore = 0;
        int negativeScore = 0;

        if (text == null || text.trim().isEmpty()) {
            return new SentimentAnalysisResult("NEUTRAL", matchedTags, 0, 0);
        }

        String cleanText = text.trim();

        if (isPureEmojiOrPunctuation(cleanText)) {
            return new SentimentAnalysisResult("NEUTRAL", matchedTags, 0, 0);
        }

        Set<String> foundKeywords = new HashSet<>();

        for (SentimentDictionary word : positiveWords) {
            if (cleanText.contains(word.getKeyword())) {
                if (!foundKeywords.contains(word.getKeyword())) {
                    positiveScore += word.getWeight();
                    matchedTags.add(word.getKeyword());
                    foundKeywords.add(word.getKeyword());
                }
            }
        }

        foundKeywords.clear();
        for (SentimentDictionary word : negativeWords) {
            if (cleanText.contains(word.getKeyword())) {
                if (!foundKeywords.contains(word.getKeyword())) {
                    negativeScore += word.getWeight();
                    matchedTags.add(word.getKeyword());
                    foundKeywords.add(word.getKeyword());
                }
            }
        }

        String sentiment;
        int threshold = 1;
        if (positiveScore - negativeScore > threshold) {
            sentiment = "POSITIVE";
        } else if (negativeScore - positiveScore > threshold) {
            sentiment = "NEGATIVE";
        } else {
            sentiment = "NEUTRAL";
        }

        logger.debug("情绪分析结果 - 文本：{}，情绪：{}，正面得分：{}，负面得分：{}，命中标签：{}",
            text, sentiment, positiveScore, negativeScore, matchedTags);

        return new SentimentAnalysisResult(sentiment, matchedTags, positiveScore, negativeScore);
    }

    private boolean isPureEmojiOrPunctuation(String text) {
        String withoutEmoji = EMOJI_PATTERN.matcher(text).replaceAll("");
        String withoutPunctuation = PUNCTUATION_PATTERN.matcher(withoutEmoji).replaceAll("");
        return withoutPunctuation.trim().isEmpty();
    }
}
