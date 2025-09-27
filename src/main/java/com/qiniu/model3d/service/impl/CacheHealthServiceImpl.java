package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.repository.ModelTaskRepository;
import com.qiniu.model3d.service.CacheHealthService;
import com.qiniu.model3d.service.CacheMetricsService;
import com.qiniu.model3d.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 缓存健康检查服务实现
 */
@Service
public class CacheHealthServiceImpl implements CacheHealthService {

    private static final Logger logger = LoggerFactory.getLogger(CacheHealthServiceImpl.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheMetricsService cacheMetricsService;

    @Autowired
    private ModelTaskRepository taskRepository;

    @Value("${cache.health.hit-rate.warning:0.5}")
    private double hitRateWarningThreshold;

    @Value("${cache.health.hit-rate.critical:0.3}")
    private double hitRateCriticalThreshold;

    @Value("${cache.health.response-time.warning:1000}")
    private double responseTimeWarningThreshold;

    @Value("${cache.health.response-time.critical:3000}")
    private double responseTimeCriticalThreshold;

    @Value("${cache.health.capacity.warning:0.8}")
    private double capacityWarningThreshold;

    @Value("${cache.health.capacity.critical:0.95}")
    private double capacityCriticalThreshold;

    @Value("${cache.max-size:10000}")
    private long maxCacheSize;

    @Override
    public CacheHealthReport performHealthCheck() {
        logger.info("Performing comprehensive cache health check");
        
        LocalDateTime checkTime = LocalDateTime.now();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();

        // 检查缓存可用性
        boolean isAvailable = isCacheAvailable();
        if (!isAvailable) {
            issues.add("缓存服务不可用");
            recommendations.add("检查缓存服务配置和连接");
        }

        // 性能检查
        CachePerformanceCheck performanceCheck = checkPerformance();
        if (performanceCheck.getPerformanceStatus() == CacheHealthStatus.WARNING) {
            issues.add("缓存性能存在警告");
            recommendations.add("考虑优化缓存策略或增加资源");
        } else if (performanceCheck.getPerformanceStatus() == CacheHealthStatus.CRITICAL) {
            issues.add("缓存性能严重下降");
            recommendations.add("立即检查系统资源和缓存配置");
        }

        // 容量检查
        CacheCapacityCheck capacityCheck = checkCapacity();
        if (capacityCheck.getCapacityStatus() == CacheHealthStatus.WARNING) {
            issues.add("缓存容量使用率较高");
            recommendations.add("考虑执行缓存清理或扩容");
        } else if (capacityCheck.getCapacityStatus() == CacheHealthStatus.CRITICAL) {
            issues.add("缓存容量严重不足");
            recommendations.add("立即执行缓存清理或扩容");
        }

        // 确定总体健康状态
        CacheHealthStatus overallStatus = determineOverallStatus(isAvailable, performanceCheck, capacityCheck);

        // 添加详细信息
        details.put("checkDuration", System.currentTimeMillis());
        details.put("cacheEnabled", true);
        details.put("lastCleanupTime", "N/A");
        details.put("lastWarmupTime", "N/A");

        // 添加通用建议
        if (issues.isEmpty()) {
            recommendations.add("缓存系统运行良好，继续监控");
        }

        CacheHealthReport report = new CacheHealthReport(
            checkTime,
            overallStatus,
            isAvailable,
            performanceCheck,
            capacityCheck,
            issues,
            recommendations,
            details
        );

        logger.info("Cache health check completed. Overall status: {}, Issues: {}", 
                   overallStatus, issues.size());
        
        return report;
    }

    @Override
    public boolean isCacheAvailable() {
        try {
            // 尝试获取缓存统计信息来测试可用性
            CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            return stats != null;
        } catch (Exception e) {
            logger.error("Cache availability check failed", e);
            return false;
        }
    }

    @Override
    public CachePerformanceCheck checkPerformance() {
        try {
            CacheMetricsService.CacheMetrics metrics = cacheMetricsService.getRealTimeMetrics();
            
            double hitRate = metrics.getHitRate();
            double avgResponseTime = metrics.getAvgResponseTime();
            long requestsPerSecond = calculateRequestsPerSecond(metrics);

            // 确定性能状态
            CacheHealthStatus performanceStatus = CacheHealthStatus.HEALTHY;
            String performanceGrade = "A";

            if (hitRate < hitRateCriticalThreshold || avgResponseTime > responseTimeCriticalThreshold) {
                performanceStatus = CacheHealthStatus.CRITICAL;
                performanceGrade = "D";
            } else if (hitRate < hitRateWarningThreshold || avgResponseTime > responseTimeWarningThreshold) {
                performanceStatus = CacheHealthStatus.WARNING;
                performanceGrade = "C";
            } else if (hitRate > 0.8 && avgResponseTime < 500) {
                performanceGrade = "A+";
            } else if (hitRate > 0.7 && avgResponseTime < 800) {
                performanceGrade = "B";
            }

            return new CachePerformanceCheck(
                hitRate,
                avgResponseTime,
                requestsPerSecond,
                performanceStatus,
                performanceGrade
            );
        } catch (Exception e) {
            logger.error("Performance check failed", e);
            return new CachePerformanceCheck(
                0.0, 0.0, 0L, CacheHealthStatus.DOWN, "F"
            );
        }
    }

    @Override
    public CacheCapacityCheck checkCapacity() {
        try {
            CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            
            long totalCapacity = maxCacheSize;
            long usedCapacity = stats.getTotalCachedTasks();
            double usagePercentage = (double) usedCapacity / totalCapacity;
            long availableCapacity = totalCapacity - usedCapacity;

            // 确定容量状态
            CacheHealthStatus capacityStatus = CacheHealthStatus.HEALTHY;
            boolean needsCleanup = false;

            if (usagePercentage >= capacityCriticalThreshold) {
                capacityStatus = CacheHealthStatus.CRITICAL;
                needsCleanup = true;
            } else if (usagePercentage >= capacityWarningThreshold) {
                capacityStatus = CacheHealthStatus.WARNING;
                needsCleanup = true;
            }

            return new CacheCapacityCheck(
                totalCapacity,
                usedCapacity,
                usagePercentage,
                availableCapacity,
                capacityStatus,
                needsCleanup
            );
        } catch (Exception e) {
            logger.error("Capacity check failed", e);
            return new CacheCapacityCheck(
                0L, 0L, 0.0, 0L, CacheHealthStatus.DOWN, false
            );
        }
    }

    @Override
    public CacheHealthStatus getHealthStatus() {
        try {
            CacheHealthReport report = performHealthCheck();
            return report.getOverallStatus();
        } catch (Exception e) {
            logger.error("Failed to get health status", e);
            return CacheHealthStatus.DOWN;
        }
    }

    // 私有辅助方法

    private CacheHealthStatus determineOverallStatus(boolean isAvailable, 
                                                   CachePerformanceCheck performanceCheck,
                                                   CacheCapacityCheck capacityCheck) {
        if (!isAvailable) {
            return CacheHealthStatus.DOWN;
        }

        // 取最严重的状态
        CacheHealthStatus worstStatus = CacheHealthStatus.HEALTHY;
        
        if (performanceCheck.getPerformanceStatus().ordinal() > worstStatus.ordinal()) {
            worstStatus = performanceCheck.getPerformanceStatus();
        }
        
        if (capacityCheck.getCapacityStatus().ordinal() > worstStatus.ordinal()) {
            worstStatus = capacityCheck.getCapacityStatus();
        }

        return worstStatus;
    }

    private long calculateRequestsPerSecond(CacheMetricsService.CacheMetrics metrics) {
        // 简化计算，实际应该基于时间窗口
        long totalRequests = metrics.getTotalRequests();
        // 假设这是过去1小时的数据
        return totalRequests / 3600; // 每秒请求数
    }

    /**
     * 执行快速健康检查（用于健康检查端点）
     */
    public Map<String, Object> performQuickHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean isAvailable = isCacheAvailable();
            CacheHealthStatus status = isAvailable ? CacheHealthStatus.HEALTHY : CacheHealthStatus.DOWN;
            
            health.put("status", status.name());
            health.put("description", status.getDescription());
            health.put("timestamp", LocalDateTime.now());
            health.put("available", isAvailable);
            
            if (isAvailable) {
                CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
                health.put("totalCachedTasks", stats.getTotalCachedTasks());
                health.put("hitRate", stats.getHitRate());
                health.put("storageSize", stats.getStorageSize());
            }
            
        } catch (Exception e) {
            health.put("status", CacheHealthStatus.DOWN.name());
            health.put("description", "健康检查失败: " + e.getMessage());
            health.put("available", false);
        }
        
        return health;
    }

    /**
     * 获取详细的健康检查信息（用于管理端点）
     */
    public Map<String, Object> getDetailedHealthInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            CacheHealthReport report = performHealthCheck();
            
            info.put("overallStatus", report.getOverallStatus().name());
            info.put("checkTime", report.getCheckTime());
            info.put("available", report.isAvailable());
            info.put("issues", report.getIssues());
            info.put("recommendations", report.getRecommendations());
            
            // 性能信息
            CachePerformanceCheck perf = report.getPerformanceCheck();
            Map<String, Object> performance = new HashMap<>();
            performance.put("hitRate", perf.getHitRate());
            performance.put("avgResponseTime", perf.getAvgResponseTime());
            performance.put("requestsPerSecond", perf.getRequestsPerSecond());
            performance.put("grade", perf.getPerformanceGrade());
            performance.put("status", perf.getPerformanceStatus().name());
            info.put("performance", performance);
            
            // 容量信息
            CacheCapacityCheck capacity = report.getCapacityCheck();
            Map<String, Object> capacityInfo = new HashMap<>();
            capacityInfo.put("totalCapacity", capacity.getTotalCapacity());
            capacityInfo.put("usedCapacity", capacity.getUsedCapacity());
            capacityInfo.put("usagePercentage", capacity.getUsagePercentage());
            capacityInfo.put("availableCapacity", capacity.getAvailableCapacity());
            capacityInfo.put("needsCleanup", capacity.isNeedsCleanup());
            capacityInfo.put("status", capacity.getCapacityStatus().name());
            info.put("capacity", capacityInfo);
            
        } catch (Exception e) {
            info.put("error", "获取详细健康信息失败: " + e.getMessage());
        }
        
        return info;
    }
}