package com.qiniu.model3d.dto;

import com.qiniu.model3d.entity.ModelTask;

/**
 * 缓存匹配结果
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public class CacheResult {
    
    private ModelTask task;
    private double similarity;
    private String matchType; // EXACT, SEMANTIC, BASIC
    private long responseTimeMs;

    public CacheResult() {}

    public CacheResult(ModelTask task, double similarity, String matchType) {
        this.task = task;
        this.similarity = similarity;
        this.matchType = matchType;
    }

    public CacheResult(ModelTask task, double similarity, String matchType, long responseTimeMs) {
        this.task = task;
        this.similarity = similarity;
        this.matchType = matchType;
        this.responseTimeMs = responseTimeMs;
    }

    // Getters and Setters
    public ModelTask getTask() {
        return task;
    }

    public void setTask(ModelTask task) {
        this.task = task;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    /**
     * 是否为精确匹配
     */
    public boolean isExactMatch() {
        return "EXACT".equals(matchType);
    }

    /**
     * 是否为语义匹配
     */
    public boolean isSemanticMatch() {
        return "SEMANTIC".equals(matchType);
    }

    /**
     * 是否为基础匹配
     */
    public boolean isBasicMatch() {
        return "BASIC".equals(matchType);
    }

    @Override
    public String toString() {
        return "CacheResult{" +
                "taskId=" + (task != null ? task.getTaskId() : "null") +
                ", similarity=" + similarity +
                ", matchType='" + matchType + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}