package com.club.dto;

import java.util.List;
import java.util.Map;

public class ActivityFeedbackStats {
    private long totalCount;
    private long positiveCount;
    private long neutralCount;
    private long negativeCount;
    private double positivePercentage;
    private double neutralPercentage;
    private double negativePercentage;
    private List<Map<String, Object>> positiveExamples;
    private List<Map<String, Object>> neutralExamples;
    private List<Map<String, Object>> negativeExamples;
    private List<Map<String, Object>> tagFrequency;
    private Double averageRating;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getPositiveCount() { return positiveCount; }
    public void setPositiveCount(long positiveCount) { this.positiveCount = positiveCount; }

    public long getNeutralCount() { return neutralCount; }
    public void setNeutralCount(long neutralCount) { this.neutralCount = neutralCount; }

    public long getNegativeCount() { return negativeCount; }
    public void setNegativeCount(long negativeCount) { this.negativeCount = negativeCount; }

    public double getPositivePercentage() { return positivePercentage; }
    public void setPositivePercentage(double positivePercentage) { this.positivePercentage = positivePercentage; }

    public double getNeutralPercentage() { return neutralPercentage; }
    public void setNeutralPercentage(double neutralPercentage) { this.neutralPercentage = neutralPercentage; }

    public double getNegativePercentage() { return negativePercentage; }
    public void setNegativePercentage(double negativePercentage) { this.negativePercentage = negativePercentage; }

    public List<Map<String, Object>> getPositiveExamples() { return positiveExamples; }
    public void setPositiveExamples(List<Map<String, Object>> positiveExamples) { this.positiveExamples = positiveExamples; }

    public List<Map<String, Object>> getNeutralExamples() { return neutralExamples; }
    public void setNeutralExamples(List<Map<String, Object>> neutralExamples) { this.neutralExamples = neutralExamples; }

    public List<Map<String, Object>> getNegativeExamples() { return negativeExamples; }
    public void setNegativeExamples(List<Map<String, Object>> negativeExamples) { this.negativeExamples = negativeExamples; }

    public List<Map<String, Object>> getTagFrequency() { return tagFrequency; }
    public void setTagFrequency(List<Map<String, Object>> tagFrequency) { this.tagFrequency = tagFrequency; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
}
