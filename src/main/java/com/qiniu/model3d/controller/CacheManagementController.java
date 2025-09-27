package com.qiniu.model3d.controller;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.service.*;
import com.qiniu.model3d.service.impl.CacheHealthServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理和监控REST API控制器
 */
@RestController
@RequestMapping("/api/cache")
public class CacheManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagementController.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheMetricsService cacheMetricsService;

    @Autowired
    private CacheHealthService cacheHealthService;

    @Autowired
    private CacheHealthServiceImpl cacheHealthServiceImpl;

    @Autowired
    private CacheWarmupService cacheWarmupService;

    // ==================== 健康检查端点 ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> quickHealthCheck() {
        try {
            Map<String, Object> health = cacheHealthServiceImpl.performQuickHealthCheck();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Quick health check failed", e);
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("description", "健康检查失败");
            errorHealth.put("error", e.getMessage());
            return ResponseEntity.status(503).body(errorHealth);
        }
    }

    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        try {
            Map<String, Object> health = cacheHealthServiceImpl.getDetailedHealthInfo();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Detailed health check failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "详细健康检查失败: " + e.getMessage()));
        }
    }

    @PostMapping("/health/check")
    public ResponseEntity<CacheHealthService.CacheHealthReport> performHealthCheck() {
        try {
            CacheHealthService.CacheHealthReport report = cacheHealthService.performHealthCheck();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== 缓存统计端点 ====================

    @GetMapping("/statistics")
    public ResponseEntity<CacheService.CacheStatistics> getCacheStatistics() {
        try {
            CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get cache statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/metrics/realtime")
    public ResponseEntity<CacheMetricsService.CacheMetrics> getRealTimeMetrics() {
        try {
            CacheMetricsService.CacheMetrics metrics = cacheMetricsService.getRealTimeMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to get real-time metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/metrics/historical")
    public ResponseEntity<List<CacheMetricsService.CacheMetrics>> getHistoricalMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<CacheMetricsService.CacheMetrics> metrics = 
                cacheMetricsService.getHistoricalMetrics(startTime, endTime);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to get historical metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/metrics/performance-report")
    public ResponseEntity<CacheMetricsService.CachePerformanceReport> getPerformanceReport(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            CacheMetricsService.CachePerformanceReport report = 
                cacheMetricsService.getPerformanceReport(hours);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Failed to get performance report", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/metrics/hotspots")
    public ResponseEntity<List<CacheMetricsService.CacheHotspot>> getCacheHotspots(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<CacheMetricsService.CacheHotspot> hotspots = 
                cacheMetricsService.getCacheHotspots(limit);
            return ResponseEntity.ok(hotspots);
        } catch (Exception e) {
            logger.error("Failed to get cache hotspots", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/metrics/trend-analysis")
    public ResponseEntity<CacheMetricsService.CacheTrendAnalysis> getTrendAnalysis(
            @RequestParam(defaultValue = "7") int days) {
        try {
            CacheMetricsService.CacheTrendAnalysis analysis = 
                cacheMetricsService.getTrendAnalysis(days);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            logger.error("Failed to get trend analysis", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== 缓存管理端点 ====================

    @PostMapping("/cleanup/force")
    public ResponseEntity<Map<String, Object>> forceCacheCleanup() {
        try {
            int cleanedCount = cacheService.forceCacheCleanup();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("cleanedCount", cleanedCount);
            result.put("message", "强制清理完成，清理了 " + cleanedCount + " 个缓存项");
            logger.info("Force cache cleanup completed, cleaned {} items", cleanedCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Force cache cleanup failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "强制清理失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/cleanup/candidates")
    public ResponseEntity<List<String>> getEvictionCandidates(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ModelTask> candidates = cacheService.getEvictionCandidates(limit);
            List<String> candidateIds = candidates.stream()
                .map(ModelTask::getTaskId)
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(candidateIds);
        } catch (Exception e) {
            logger.error("Failed to get eviction candidates", e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> forceEvictTask(
            @PathVariable String taskId) {
        try {
            boolean evicted = cacheService.forceEvictTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", evicted);
            result.put("taskId", taskId);
            result.put("message", evicted ? "任务已成功从缓存中淘汰" : "任务不在缓存中或淘汰失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to evict task: {}", taskId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "淘汰任务失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/cleanup/statistics")
    public ResponseEntity<Map<String, Object>> getCacheEvictionStatistics() {
        try {
            CacheEvictionService.CacheStatistics stats = cacheService.getCacheEvictionStatistics();
            Map<String, Object> result = new HashMap<>();
            result.put("totalCacheSize", stats.getTotalCacheSize());
            result.put("totalCacheCount", stats.getTotalCacheCount());
            result.put("availableSpace", stats.getAvailableSpace());
            result.put("cacheHitRate", stats.getCacheHitRate());
            result.put("oldestCacheAge", stats.getOldestCacheAge());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get eviction statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== 缓存预热端点 ====================

    @PostMapping("/warmup")
    public ResponseEntity<Map<String, Object>> performWarmup(
            @RequestParam(required = false) String strategy) {
        try {
            // 使用CacheWarmupService直接执行预热，忽略strategy参数
            int warmedCount = cacheWarmupService.performWarmup();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("warmedCount", warmedCount);
            result.put("strategy", strategy != null ? strategy : "AUTO");
            result.put("message", "缓存预热完成，预热了 " + warmedCount + " 个任务");
            logger.info("Cache warmup completed, warmed {} tasks with strategy {}", 
                       warmedCount, strategy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Cache warmup failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "缓存预热失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/warmup/statistics")
    public ResponseEntity<CacheWarmupService.WarmupStatistics> getWarmupStatistics() {
        try {
            CacheWarmupService.WarmupStatistics stats = cacheService.getWarmupStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get warmup statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/warmup/candidates")
    public ResponseEntity<List<String>> getWarmupCandidates(
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            // 转换字符串策略为枚举
            CacheWarmupService.WarmupStrategy warmupStrategy = null;
            if (strategy != null) {
                try {
                    warmupStrategy = CacheWarmupService.WarmupStrategy.valueOf(strategy.toUpperCase());
                } catch (IllegalArgumentException e) {
                    warmupStrategy = CacheWarmupService.WarmupStrategy.COMPREHENSIVE;
                }
            } else {
                warmupStrategy = CacheWarmupService.WarmupStrategy.COMPREHENSIVE;
            }
            
            List<String> candidates = cacheWarmupService.getWarmupCandidates(warmupStrategy, limit)
                .stream()
                .map(task -> task.getTaskId())
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(candidates);
        } catch (Exception e) {
            logger.error("Failed to get warmup candidates", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== 系统管理端点 ====================

    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        try {
            cacheMetricsService.resetMetrics();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "缓存指标已重置");
            result.put("resetTime", LocalDateTime.now());
            logger.info("Cache metrics reset by admin");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to reset metrics", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "重置指标失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 基本状态
            CacheHealthService.CacheHealthStatus healthStatus = cacheHealthService.getHealthStatus();
            status.put("healthStatus", healthStatus.name());
            status.put("healthDescription", healthStatus.getDescription());
            
            // 统计信息
            CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            status.put("totalCachedTasks", stats.getTotalCachedTasks());
            status.put("hitRate", stats.getHitRate());
            status.put("storageSize", stats.getStorageSize());
            
            // 实时指标
            CacheMetricsService.CacheMetrics metrics = cacheMetricsService.getRealTimeMetrics();
            status.put("totalRequests", metrics.getTotalRequests());
            status.put("avgResponseTime", metrics.getAvgResponseTime());
            
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get cache status", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "获取缓存状态失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getCacheConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("maxCacheSize", "配置值");
            config.put("cleanupInterval", "配置值");
            config.put("warmupInterval", "配置值");
            config.put("hitRateThreshold", "配置值");
            config.put("capacityThreshold", "配置值");
            // 这里应该从配置服务获取实际配置值
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("Failed to get cache config", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "获取缓存配置失败: " + e.getMessage()
            ));
        }
    }
}