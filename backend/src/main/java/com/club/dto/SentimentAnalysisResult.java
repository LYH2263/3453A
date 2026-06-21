package com.club.dto;

import java.util.List;

public class SentimentAnalysisResult {
    private String sentiment;
    private List<String> tags;
    private int positiveScore;
    private int negativeScore;

    public SentimentAnalysisResult(String sentiment, List<String> tags, int positiveScore, int negativeScore) {
        this.sentiment = sentiment;
        this.tags = tags;
        this.positiveScore = positiveScore;
        this.negativeScore = negativeScore;
    }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public int getPositiveScore() { return positiveScore; }
    public void setPositiveScore(int positiveScore) { this.positiveScore = positiveScore; }

    public int getNegativeScore() { return negativeScore; }
    public void setNegativeScore(int negativeScore) { this.negativeScore = negativeScore; }
}
