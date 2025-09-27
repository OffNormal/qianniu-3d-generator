package com.qiniu.model3d.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 缓存指标服务接口
 * 负责收集、分析和报告缓存性能指标
 */
public interface CacheMetricsService {

    /**
     * 记录缓存命中
     * 
     * @param taskId 任务ID
     * @param hitType 命中类型（EXACT, SIMILAR）
     * @param responseTime 响应时间（毫秒）
     */
    void recordCacheHit(String taskId, String hitType, long responseTime);

    /**
     * 记录缓存未命中
     * 
     * @param inputHash 输入哈希
     * @param taskType 任务类型
     * @param responseTime 响应时间（毫秒）
     */
    void recordCacheMiss(String inputHash, String taskType, long responseTime);

    /**
     * 记录缓存操作
     * 
     * @param operation 操作类型（STORE, EVICT, WARMUP）
     * @param taskId 任务ID
     * @param duration 操作耗时（毫秒）
     */
    void recordCacheOperation(String operation, String taskId, long duration);

    /**
     * 获取实时缓存指标
     * 
     * @return 实时指标
     */
    CacheMetrics getRealTimeMetrics();

    /**
     * 获取历史缓存指标
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 历史指标列表
     */
    List<CacheMetrics> getHistoricalMetrics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取缓存性能报告
     * 
     * @param hours 过去几小时的数据
     * @return 性能报告
     */
    CachePerformanceReport getPerformanceReport(int hours);

    /**
     * 获取缓存热点分析
     * 
     * @param topN 返回前N个热点
     * @return 热点分析结果
     */
    List<CacheHotspot> getCacheHotspots(int topN);

    /**
     * 获取缓存趋势分析
     * 
     * @param days 过去几天的数据
     * @return 趋势分析结果
     */
    CacheTrendAnalysis getTrendAnalysis(int days);

    /**
     * 重置指标统计
     */
    void resetMetrics();

    /**
     * 缓存指标数据类
     */
    class CacheMetrics {
        private LocalDateTime timestamp;
        private long totalRequests;
        private long cacheHits;
        private long cacheMisses;
        private double hitRate;
        private double avgResponseTime;
        private long totalCachedTasks;
        private long memoryUsageBytes;
        private Map<String, Long> hitsByType;
        private Map<String, Long> operationCounts;

        public CacheMetrics() {}

        public CacheMetrics(LocalDateTime timestamp, long totalRequests, long cacheHits, 
                           long cacheMisses, double hitRate, double avgResponseTime,
                           long totalCachedTasks, long memoryUsageBytes,
                           Map<String, Long> hitsByType, Map<String, Long> operationCounts) {
            this.timestamp = timestamp;
            this.totalRequests = totalRequests;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.hitRate = hitRate;
            this.avgResponseTime = avgResponseTime;
            this.totalCachedTasks = totalCachedTasks;
            this.memoryUsageBytes = memoryUsageBytes;
            this.hitsByType = hitsByType;
            this.operationCounts = operationCounts;
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }

        public long getCacheMisses() { return cacheMisses; }
        public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }

        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

        public long getTotalCachedTasks() { return totalCachedTasks; }
        public void setTotalCachedTasks(long totalCachedTasks) { this.totalCachedTasks = totalCachedTasks; }

        public long getMemoryUsageBytes() { return memoryUsageBytes; }
        public void setMemoryUsageBytes(long memoryUsageBytes) { this.memoryUsageBytes = memoryUsageBytes; }

        public Map<String, Long> getHitsByType() { return hitsByType; }
        public void setHitsByType(Map<String, Long> hitsByType) { this.hitsByType = hitsByType; }

        public Map<String, Long> getOperationCounts() { return operationCounts; }
        public void setOperationCounts(Map<String, Long> operationCounts) { this.operationCounts = operationCounts; }
    }

    /**
     * 缓存性能报告
     */
    class CachePerformanceReport {
        private LocalDateTime reportTime;
        private int periodHours;
        private double overallHitRate;
        private double avgResponseTime;
        private long totalRequests;
        private long peakRequestsPerHour;
        private Map<String, Double> hitRateByType;
        private Map<String, Double> avgResponseTimeByType;
        private List<String> recommendations;

        public CachePerformanceReport() {}

        public CachePerformanceReport(LocalDateTime reportTime, int periodHours, 
                                    double overallHitRate, double avgResponseTime,
                                    long totalRequests, long peakRequestsPerHour,
                                    Map<String, Double> hitRateByType,
                                    Map<String, Double> avgResponseTimeByType,
                                    List<String> recommendations) {
            this.reportTime = reportTime;
            this.periodHours = periodHours;
            this.overallHitRate = overallHitRate;
            this.avgResponseTime = avgResponseTime;
            this.totalRequests = totalRequests;
            this.peakRequestsPerHour = peakRequestsPerHour;
            this.hitRateByType = hitRateByType;
            this.avgResponseTimeByType = avgResponseTimeByType;
            this.recommendations = recommendations;
        }

        // Getters and Setters
        public LocalDateTime getReportTime() { return reportTime; }
        public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }

        public int getPeriodHours() { return periodHours; }
        public void setPeriodHours(int periodHours) { this.periodHours = periodHours; }

        public double getOverallHitRate() { return overallHitRate; }
        public void setOverallHitRate(double overallHitRate) { this.overallHitRate = overallHitRate; }

        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getPeakRequestsPerHour() { return peakRequestsPerHour; }
        public void setPeakRequestsPerHour(long peakRequestsPerHour) { this.peakRequestsPerHour = peakRequestsPerHour; }

        public Map<String, Double> getHitRateByType() { return hitRateByType; }
        public void setHitRateByType(Map<String, Double> hitRateByType) { this.hitRateByType = hitRateByType; }

        public Map<String, Double> getAvgResponseTimeByType() { return avgResponseTimeByType; }
        public void setAvgResponseTimeByType(Map<String, Double> avgResponseTimeByType) { this.avgResponseTimeByType = avgResponseTimeByType; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    /**
     * 缓存热点
     */
    class CacheHotspot {
        private String taskId;
        private String inputHash;
        private String taskType;
        private long hitCount;
        private double hitRate;
        private LocalDateTime lastAccessed;
        private double avgResponseTime;

        public CacheHotspot() {}

        public CacheHotspot(String taskId, String inputHash, String taskType,
                           long hitCount, double hitRate, LocalDateTime lastAccessed,
                           double avgResponseTime) {
            this.taskId = taskId;
            this.inputHash = inputHash;
            this.taskType = taskType;
            this.hitCount = hitCount;
            this.hitRate = hitRate;
            this.lastAccessed = lastAccessed;
            this.avgResponseTime = avgResponseTime;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getInputHash() { return inputHash; }
        public void setInputHash(String inputHash) { this.inputHash = inputHash; }

        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }

        public long getHitCount() { return hitCount; }
        public void setHitCount(long hitCount) { this.hitCount = hitCount; }

        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }

        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    }

    /**
     * 缓存趋势分析
     */
    class CacheTrendAnalysis {
        private LocalDateTime analysisTime;
        private int periodDays;
        private double hitRateTrend; // 正数表示上升趋势，负数表示下降趋势
        private double responseTimeTrend;
        private double requestVolumeTrend;
        private Map<String, Double> trendsByType;
        private List<String> insights;

        public CacheTrendAnalysis() {}

        public CacheTrendAnalysis(LocalDateTime analysisTime, int periodDays,
                                 double hitRateTrend, double responseTimeTrend,
                                 double requestVolumeTrend, Map<String, Double> trendsByType,
                                 List<String> insights) {
            this.analysisTime = analysisTime;
            this.periodDays = periodDays;
            this.hitRateTrend = hitRateTrend;
            this.responseTimeTrend = responseTimeTrend;
            this.requestVolumeTrend = requestVolumeTrend;
            this.trendsByType = trendsByType;
            this.insights = insights;
        }

        // Getters and Setters
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }

        public int getPeriodDays() { return periodDays; }
        public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }

        public double getHitRateTrend() { return hitRateTrend; }
        public void setHitRateTrend(double hitRateTrend) { this.hitRateTrend = hitRateTrend; }

        public double getResponseTimeTrend() { return responseTimeTrend; }
        public void setResponseTimeTrend(double responseTimeTrend) { this.responseTimeTrend = responseTimeTrend; }

        public double getRequestVolumeTrend() { return requestVolumeTrend; }
        public void setRequestVolumeTrend(double requestVolumeTrend) { this.requestVolumeTrend = requestVolumeTrend; }

        public Map<String, Double> getTrendsByType() { return trendsByType; }
        public void setTrendsByType(Map<String, Double> trendsByType) { this.trendsByType = trendsByType; }

        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }
}