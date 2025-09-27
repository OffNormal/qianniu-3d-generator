package com.qiniu.model3d.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 缓存调度服务
 * 负责定时执行缓存清理和预热任务
 */
@Service
@ConditionalOnProperty(name = "cache.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CacheSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(CacheSchedulerService.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Autowired
    private com.qiniu.model3d.service.CacheMetricsService cacheMetricsService;

    /**
     * 定时检查并执行缓存清理
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
    public void scheduledCacheCleanup() {
        try {
            logger.info("Starting scheduled cache cleanup check");
            
            if (cacheService.shouldPerformCleanup()) {
                int cleanedCount = cacheService.forceCacheCleanup();
                logger.info("Scheduled cache cleanup completed, cleaned {} tasks", cleanedCount);
            } else {
                logger.debug("Cache cleanup not needed at this time");
            }
        } catch (Exception e) {
            logger.error("Error during scheduled cache cleanup", e);
        }
    }

    /**
     * 定时执行缓存预热
     * 每2小时执行一次
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2小时
    public void scheduledCacheWarmup() {
        try {
            logger.info("Starting scheduled cache warmup");
            
            if (cacheWarmupService.shouldPerformWarmup()) {
                int warmedCount = cacheService.performIntelligentWarmup();
                logger.info("Scheduled cache warmup completed, warmed {} tasks", warmedCount);
            } else {
                logger.debug("Cache warmup not needed at this time");
            }
        } catch (Exception e) {
            logger.error("Error during scheduled cache warmup", e);
        }
    }

    /**
     * 定时输出缓存统计信息
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1小时
    public void scheduledCacheStatistics() {
        try {
            logger.info("=== Cache Statistics Report ===");
            
            // 缓存统计
            CacheService.CacheStatistics cacheStats = cacheService.getCacheStatistics();
            logger.info("Cache Stats - Total: {}, Hit Rate: {:.2f}%, Memory Usage: {:.2f}MB", 
                cacheStats.getTotalCachedTasks(), 
                cacheStats.getHitRate() * 100,
                cacheStats.getStorageSize() / (1024.0 * 1024.0));
            
            // 淘汰统计
            CacheEvictionService.CacheStatistics evictionStats = cacheService.getCacheEvictionStatistics();
            logger.info("Cache Stats - Total Size: {}MB, Count: {}, Available Space: {}MB, Hit Rate: {:.2f}%, Oldest Age: {}h", 
                evictionStats.getTotalCacheSize() / (1024 * 1024),
                evictionStats.getTotalCacheCount(),
                evictionStats.getAvailableSpace() / (1024 * 1024),
                evictionStats.getCacheHitRate() * 100,
                evictionStats.getOldestCacheAge() / 3600000); // 转换为小时
            
            // 预热统计
            CacheWarmupService.WarmupStatistics warmupStats = cacheService.getWarmupStatistics();
            logger.info("Warmup Stats - Total Warmed: {}, Popular: {}, Time Pattern: {}, User Behavior: {}, Similar: {}", 
                warmupStats.getTotalWarmedTasks(),
                warmupStats.getPopularTasksWarmed(),
                warmupStats.getTimePatternWarmed(),
                warmupStats.getUserBehaviorWarmed(),
                warmupStats.getSimilarTasksWarmed());
            
            // 实时指标
            com.qiniu.model3d.service.CacheMetricsService.CacheMetrics realTimeMetrics = cacheMetricsService.getRealTimeMetrics();
            logger.info("Real-time Metrics - Requests: {}, Hit Rate: {:.2f}%, Avg Response Time: {:.2f}ms", 
                realTimeMetrics.getTotalRequests(),
                realTimeMetrics.getHitRate() * 100,
                realTimeMetrics.getAvgResponseTime());
            
            logger.info("=== End Cache Statistics Report ===");
        } catch (Exception e) {
            logger.error("Error during scheduled cache statistics", e);
        }
    }

    /**
     * 定期保存指标到历史记录
     * 每15分钟执行一次
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15分钟
    public void scheduledMetricsCollection() {
        try {
            if (cacheMetricsService instanceof com.qiniu.model3d.service.impl.CacheMetricsServiceImpl) {
                ((com.qiniu.model3d.service.impl.CacheMetricsServiceImpl) cacheMetricsService)
                    .saveCurrentMetricsToHistory();
                logger.debug("Saved current metrics to history");
            }
        } catch (Exception e) {
            logger.error("Error during scheduled metrics collection", e);
        }
    }

    /**
     * 深夜缓存维护任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void nightlyMaintenance() {
        try {
            logger.info("Starting nightly cache maintenance");
            
            // 强制执行一次全面清理
            int cleanedCount = cacheService.forceCacheCleanup();
            logger.info("Nightly cleanup completed, cleaned {} tasks", cleanedCount);
            
            // 预热热门任务
            int warmedCount = cacheWarmupService.warmupPopularTasks(7, 50);
            logger.info("Nightly warmup completed, warmed {} popular tasks", warmedCount);
            
            logger.info("Nightly cache maintenance completed");
        } catch (Exception e) {
            logger.error("Error during nightly cache maintenance", e);
        }
    }
}