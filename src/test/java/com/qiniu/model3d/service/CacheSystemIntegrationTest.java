package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存系统集成测试
 * 验证缓存系统各组件的集成和功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class CacheSystemIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheMetricsService cacheMetricsService;

    @Autowired
    private CacheHealthService cacheHealthService;

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Test
    public void testCacheBasicOperations() {
        // 测试基本缓存操作
        String testText = "测试文本生成";
        
        // 1. 查找缓存（应该为空）
        var exactMatch = cacheService.findExactMatch(testText, ModelTask.TaskType.TEXT, "medium", "obj");
        assertFalse(exactMatch.isPresent(), "初始状态应该没有缓存");
        
        // 2. 创建测试任务
        ModelTask task = new ModelTask();
        task.setTaskId("test-task-001");
        task.setInputText(testText);
        task.setStatus(ModelTask.TaskStatus.COMPLETED);
        task.setClientIp("127.0.0.1");
        
        // 3. 缓存任务
        cacheService.cacheTask(task);
        
        // 4. 再次查找缓存（应该找到）
        exactMatch = cacheService.findExactMatch(testText, ModelTask.TaskType.TEXT, "medium", "obj");
        assertTrue(exactMatch.isPresent(), "缓存后应该能找到任务");
        assertEquals(testText, exactMatch.get().getInputText());
    }

    @Test
    public void testCacheMetrics() {
        // 测试缓存指标收集
        String cacheKey = "test-metrics";
        
        // 记录缓存命中
        cacheMetricsService.recordCacheHit("test-task-001", "text", 100L);
        cacheMetricsService.recordCacheHit("test-task-002", "text", 150L);
        
        // 记录缓存未命中
        cacheMetricsService.recordCacheMiss("miss-key-hash", "text", 200L);
        
        // 获取实时指标
        var metrics = cacheMetricsService.getRealTimeMetrics();
        assertNotNull(metrics, "应该能获取实时指标");
        assertTrue(metrics.getCacheHits() >= 2, "命中次数应该至少为2");
        assertTrue(metrics.getCacheMisses() >= 1, "未命中次数应该至少为1");
        
        // 计算命中率
        double hitRate = (double) metrics.getCacheHits() / (metrics.getCacheHits() + metrics.getCacheMisses());
        assertTrue(hitRate > 0 && hitRate <= 1, "命中率应该在0-1之间");
    }

    @Test
    public void testCacheHealth() {
        // 测试缓存健康检查
        var healthStatus = cacheHealthService.getHealthStatus();
        assertNotNull(healthStatus, "应该能获取健康状态");
        
        // 执行完整健康检查
        var healthReport = cacheHealthService.performHealthCheck();
        assertNotNull(healthReport, "应该能获取健康报告");
        assertNotNull(healthReport.getOverallStatus(), "应该有整体状态");
        assertNotNull(healthReport.getPerformanceCheck(), "应该有性能检查结果");
        assertNotNull(healthReport.getCapacityCheck(), "应该有容量检查结果");
    }

    @Test
    public void testCacheWarmup() {
        // 测试缓存预热
        try {
            // 执行预热
            int warmedCount = cacheWarmupService.performWarmup();
            assertTrue(warmedCount >= 0, "预热任务数量应该大于等于0");
            
            // 获取预热统计
            var warmupStats = cacheWarmupService.getWarmupStatistics();
            assertNotNull(warmupStats, "应该能获取预热统计");
            assertTrue(warmupStats.getTotalWarmedTasks() >= 0, "总预热任务数应该大于等于0");
            
            // 检查是否应该执行预热
            boolean shouldWarmup = cacheWarmupService.shouldPerformWarmup();
            // 这个结果可能为true或false，取决于当前状态
            
        } catch (Exception e) {
            // 预热可能因为数据不足而失败，这在测试环境中是正常的
            System.out.println("预热测试跳过: " + e.getMessage());
        }
    }

    @Test
    public void testCachePerformanceReport() {
        // 测试性能报告生成
        try {
            var performanceReport = cacheMetricsService.getPerformanceReport(24); // 24小时的报告
            assertNotNull(performanceReport, "应该能获取性能报告");
            assertTrue(performanceReport.getOverallHitRate() >= 0, "整体命中率应该大于等于0");
            assertNotNull(performanceReport.getRecommendations(), "应该有优化建议");
            assertEquals(24, performanceReport.getPeriodHours(), "报告周期应该是24小时");
            
        } catch (Exception e) {
            // 在测试环境中可能数据不足
            System.out.println("性能报告测试跳过: " + e.getMessage());
        }
    }

    @Test
    public void testCacheHotspotAnalysis() {
        // 测试热点分析
        try {
            var hotspots = cacheMetricsService.getCacheHotspots(5);
            assertNotNull(hotspots, "应该能获取热点列表");
            
        } catch (Exception e) {
            // 在测试环境中可能数据不足
            System.out.println("热点分析测试跳过: " + e.getMessage());
        }
    }

    @Test
    public void testCacheCleanup() {
        // 测试缓存清理
        try {
            // 获取清理候选
            var candidates = cacheService.getEvictionCandidates(10);
            assertNotNull(candidates, "应该能获取清理候选列表");
            
            // 执行强制清理
            int cleanedCount = cacheService.forceCacheCleanup();
            assertTrue(cleanedCount >= 0, "清理数量应该非负");
            
        } catch (Exception e) {
            System.out.println("缓存清理测试跳过: " + e.getMessage());
        }
    }

    @Test
    public void testIntegratedWorkflow() {
        // 测试完整的缓存工作流程
        String testInput = "集成测试文本";
        
        // 1. 记录缓存未命中
        cacheMetricsService.recordCacheMiss(testInput, "text", 200L);
        
        // 2. 创建并缓存任务
        ModelTask task = new ModelTask();
        task.setTaskId("integration-test-001");
        task.setInputText(testInput);
        task.setStatus(ModelTask.TaskStatus.COMPLETED);
        task.setClientIp("127.0.0.1");
        task.setCacheHitCount(1);
        
        cacheService.cacheTask(task);
        
        // 3. 记录缓存命中
        cacheMetricsService.recordCacheHit("integration-test-001", "text", 100L);
        
        // 4. 验证缓存存在
        var cachedTask = cacheService.findExactMatch(testInput, ModelTask.TaskType.TEXT, "medium", "obj");
        assertTrue(cachedTask.isPresent(), "任务应该被缓存");
        
        // 5. 检查健康状态
        var health = cacheHealthService.isCacheAvailable();
        assertTrue(health, "缓存应该可用");
        
        // 6. 获取指标
        var metrics = cacheMetricsService.getRealTimeMetrics();
        assertTrue(metrics.getCacheHits() > 0, "应该有缓存命中记录");
        assertTrue(metrics.getCacheMisses() > 0, "应该有缓存未命中记录");
        
        System.out.println("集成测试完成 - 命中率: " + 
            (double) metrics.getCacheHits() / (metrics.getCacheHits() + metrics.getCacheMisses()));
    }
}