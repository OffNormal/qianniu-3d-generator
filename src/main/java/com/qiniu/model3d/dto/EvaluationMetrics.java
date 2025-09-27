package com.qiniu.model3d.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 评估指标数据传输对象
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public class EvaluationMetrics {

    // 基础统计数据
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalTasks;
    private Integer successTasks;
    private Integer failedTasks;
    private BigDecimal successRate;
    private BigDecimal avgDurationSeconds;
    private Integer totalUsers;
    private BigDecimal avgRating;
    private Integer totalDownloads;
    private Integer totalPreviews;
    private BigDecimal downloadRate;

    // 趋势数据
    private List<DailyMetrics> dailyTrends;
    
    // 分布数据
    private Map<String, Integer> formatDistribution;
    private Map<Integer, Integer> ratingDistribution;
    private List<String> popularPrompts;

    // 构造函数
    public EvaluationMetrics() {
    }

    public EvaluationMetrics(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 内部类：日度指标
    public static class DailyMetrics {
        private LocalDate date;
        private Integer tasks;
        private BigDecimal successRate;
        private BigDecimal avgRating;
        private Integer downloads;

        public DailyMetrics() {
        }

        public DailyMetrics(LocalDate date, Integer tasks, BigDecimal successRate) {
            this.date = date;
            this.tasks = tasks;
            this.successRate = successRate;
        }

        // Getters and Setters
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Integer getTasks() {
            return tasks;
        }

        public void setTasks(Integer tasks) {
            this.tasks = tasks;
        }

        public BigDecimal getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(BigDecimal successRate) {
            this.successRate = successRate;
        }

        public BigDecimal getAvgRating() {
            return avgRating;
        }

        public void setAvgRating(BigDecimal avgRating) {
            this.avgRating = avgRating;
        }

        public Integer getDownloads() {
            return downloads;
        }

        public void setDownloads(Integer downloads) {
            this.downloads = downloads;
        }
    }

    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getSuccessTasks() {
        return successTasks;
    }

    public void setSuccessTasks(Integer successTasks) {
        this.successTasks = successTasks;
    }

    public Integer getFailedTasks() {
        return failedTasks;
    }

    public void setFailedTasks(Integer failedTasks) {
        this.failedTasks = failedTasks;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public BigDecimal getAvgDurationSeconds() {
        return avgDurationSeconds;
    }

    public void setAvgDurationSeconds(BigDecimal avgDurationSeconds) {
        this.avgDurationSeconds = avgDurationSeconds;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(Integer totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public Integer getTotalPreviews() {
        return totalPreviews;
    }

    public void setTotalPreviews(Integer totalPreviews) {
        this.totalPreviews = totalPreviews;
    }

    public BigDecimal getDownloadRate() {
        return downloadRate;
    }

    public void setDownloadRate(BigDecimal downloadRate) {
        this.downloadRate = downloadRate;
    }

    public List<DailyMetrics> getDailyTrends() {
        return dailyTrends;
    }

    public void setDailyTrends(List<DailyMetrics> dailyTrends) {
        this.dailyTrends = dailyTrends;
    }

    public Map<String, Integer> getFormatDistribution() {
        return formatDistribution;
    }

    public void setFormatDistribution(Map<String, Integer> formatDistribution) {
        this.formatDistribution = formatDistribution;
    }

    public Map<Integer, Integer> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public List<String> getPopularPrompts() {
        return popularPrompts;
    }

    public void setPopularPrompts(List<String> popularPrompts) {
        this.popularPrompts = popularPrompts;
    }

    @Override
    public String toString() {
        return "EvaluationMetrics{" +
                "totalTasks=" + totalTasks +
                ", successRate=" + successRate +
                ", avgRating=" + avgRating +
                ", downloadRate=" + downloadRate +
                '}';
    }
}