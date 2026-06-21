package com.club.service;

import com.club.dto.SentimentAnalysisResult;

public interface SentimentAnalysisService {
    SentimentAnalysisResult analyze(String text);
    void reloadDictionary();
}
