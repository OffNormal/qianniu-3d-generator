package com.qiniu.model3d.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 缓存健康检查服务接口
 */
public interface CacheHealthService {

    /**
     * 执行完整的缓存健康检查
     * 
     * @return 健康检查结果
     */
    CacheHealthReport performHealthCheck();

    /**
     * 检查缓存可用性
     * 
     * @return 是否可用
     */
    boolean isCacheAvailable();

    /**
     * 检查缓存性能
     * 
     * @return 性能检查结果
     */
    CachePerformanceCheck checkPerformance();

    /**
     * 检查缓存容量
     * 
     * @return 容量检查结果
     */
    CacheCapacityCheck checkCapacity();

    /**
     * 获取缓存健康状态
     * 
     * @return 健康状态
     */
    CacheHealthStatus getHealthStatus();

    /**
     * 缓存健康报告
     */
    class CacheHealthReport {
        private LocalDateTime checkTime;
        private CacheHealthStatus overallStatus;
        private boolean isAvailable;
        private CachePerformanceCheck performanceCheck;
        private CacheCapacityCheck capacityCheck;
        private List<String> issues;
        private List<String> recommendations;
        private Map<String, Object> details;

        public CacheHealthReport() {}

        public CacheHealthReport(LocalDateTime checkTime, CacheHealthStatus overallStatus,
                               boolean isAvailable, CachePerformanceCheck performanceCheck,
                               CacheCapacityCheck capacityCheck, List<String> issues,
                               List<String> recommendations, Map<String, Object> details) {
            this.checkTime = checkTime;
            this.overallStatus = overallStatus;
            this.isAvailable = isAvailable;
            this.performanceCheck = performanceCheck;
            this.capacityCheck = capacityCheck;
            this.issues = issues;
            this.recommendations = recommendations;
            this.details = details;
        }

        // Getters and Setters
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

        public CacheHealthStatus getOverallStatus() { return overallStatus; }
        public void setOverallStatus(CacheHealthStatus overallStatus) { this.overallStatus = overallStatus; }

        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }

        public CachePerformanceCheck getPerformanceCheck() { return performanceCheck; }
        public void setPerformanceCheck(CachePerformanceCheck performanceCheck) { this.performanceCheck = performanceCheck; }

        public CacheCapacityCheck getCapacityCheck() { return capacityCheck; }
        public void setCapacityCheck(CacheCapacityCheck capacityCheck) { this.capacityCheck = capacityCheck; }

        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    /**
     * 缓存性能检查
     */
    class CachePerformanceCheck {
        private double hitRate;
        private double avgResponseTime;
        private long requestsPerSecond;
        private CacheHealthStatus performanceStatus;
        private String performanceGrade;

        public CachePerformanceCheck() {}

        public CachePerformanceCheck(double hitRate, double avgResponseTime,
                                   long requestsPerSecond, CacheHealthStatus performanceStatus,
                                   String performanceGrade) {
            this.hitRate = hitRate;
            this.avgResponseTime = avgResponseTime;
            this.requestsPerSecond = requestsPerSecond;
            this.performanceStatus = performanceStatus;
            this.performanceGrade = performanceGrade;
        }

        // Getters and Setters
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

        public long getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(long requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }

        public CacheHealthStatus getPerformanceStatus() { return performanceStatus; }
        public void setPerformanceStatus(CacheHealthStatus performanceStatus) { this.performanceStatus = performanceStatus; }

        public String getPerformanceGrade() { return performanceGrade; }
        public void setPerformanceGrade(String performanceGrade) { this.performanceGrade = performanceGrade; }
    }

    /**
     * 缓存容量检查
     */
    class CacheCapacityCheck {
        private long totalCapacity;
        private long usedCapacity;
        private double usagePercentage;
        private long availableCapacity;
        private CacheHealthStatus capacityStatus;
        private boolean needsCleanup;

        public CacheCapacityCheck() {}

        public CacheCapacityCheck(long totalCapacity, long usedCapacity,
                                double usagePercentage, long availableCapacity,
                                CacheHealthStatus capacityStatus, boolean needsCleanup) {
            this.totalCapacity = totalCapacity;
            this.usedCapacity = usedCapacity;
            this.usagePercentage = usagePercentage;
            this.availableCapacity = availableCapacity;
            this.capacityStatus = capacityStatus;
            this.needsCleanup = needsCleanup;
        }

        // Getters and Setters
        public long getTotalCapacity() { return totalCapacity; }
        public void setTotalCapacity(long totalCapacity) { this.totalCapacity = totalCapacity; }

        public long getUsedCapacity() { return usedCapacity; }
        public void setUsedCapacity(long usedCapacity) { this.usedCapacity = usedCapacity; }

        public double getUsagePercentage() { return usagePercentage; }
        public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }

        public long getAvailableCapacity() { return availableCapacity; }
        public void setAvailableCapacity(long availableCapacity) { this.availableCapacity = availableCapacity; }

        public CacheHealthStatus getCapacityStatus() { return capacityStatus; }
        public void setCapacityStatus(CacheHealthStatus capacityStatus) { this.capacityStatus = capacityStatus; }

        public boolean isNeedsCleanup() { return needsCleanup; }
        public void setNeedsCleanup(boolean needsCleanup) { this.needsCleanup = needsCleanup; }
    }

    /**
     * 缓存健康状态枚举
     */
    enum CacheHealthStatus {
        HEALTHY("健康"),
        WARNING("警告"),
        CRITICAL("严重"),
        DOWN("不可用");

        private final String description;

        CacheHealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}