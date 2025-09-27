package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelTaskRepository;
import com.qiniu.model3d.service.CacheMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 缓存指标服务实现
 */
@Service
public class CacheMetricsServiceImpl implements CacheMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsServiceImpl.class);

    @Autowired
    private ModelTaskRepository taskRepository;

    // 实时指标统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final Map<String, AtomicLong> hitsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimes = new ConcurrentHashMap<>();
    
    // 历史指标存储（简化实现，实际应该存储到数据库）
    private final List<CacheMetrics> historicalMetrics = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void recordCacheHit(String taskId, String hitType, long responseTime) {
        totalRequests.incrementAndGet();
        cacheHits.incrementAndGet();
        hitsByType.computeIfAbsent(hitType, k -> new AtomicLong(0)).incrementAndGet();
        
        responseTimes.computeIfAbsent("HIT_" + hitType, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(responseTime);
        
        logger.debug("Recorded cache hit: taskId={}, hitType={}, responseTime={}ms", 
                    taskId, hitType, responseTime);
    }

    @Override
    public void recordCacheMiss(String inputHash, String taskType, long responseTime) {
        totalRequests.incrementAndGet();
        cacheMisses.incrementAndGet();
        
        responseTimes.computeIfAbsent("MISS_" + taskType, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(responseTime);
        
        logger.debug("Recorded cache miss: inputHash={}, taskType={}, responseTime={}ms", 
                    inputHash, taskType, responseTime);
    }

    @Override
    public void recordCacheOperation(String operation, String taskId, long duration) {
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        
        responseTimes.computeIfAbsent("OP_" + operation, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(duration);
        
        logger.debug("Recorded cache operation: operation={}, taskId={}, duration={}ms", 
                    operation, taskId, duration);
    }

    @Override
    public CacheMetrics getRealTimeMetrics() {
        long totalReqs = totalRequests.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        double hitRate = totalReqs > 0 ? (double) hits / totalReqs : 0.0;
        
        // 计算平均响应时间
        double avgResponseTime = calculateAverageResponseTime();
        
        // 获取当前缓存任务数量
        long totalCachedTasks = taskRepository.countByCachedTrue();
        
        // 估算内存使用（简化计算）
        long memoryUsageBytes = estimateMemoryUsage();
        
        // 构建按类型统计
        Map<String, Long> hitsByTypeMap = hitsByType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        
        Map<String, Long> operationCountsMap = operationCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        
        return new CacheMetrics(
            LocalDateTime.now(),
            totalReqs,
            hits,
            misses,
            hitRate,
            avgResponseTime,
            totalCachedTasks,
            memoryUsageBytes,
            hitsByTypeMap,
            operationCountsMap
        );
    }

    @Override
    public List<CacheMetrics> getHistoricalMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        return historicalMetrics.stream()
                .filter(m -> m.getTimestamp().isAfter(startTime) && m.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public CachePerformanceReport getPerformanceReport(int hours) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        
        List<CacheMetrics> metrics = getHistoricalMetrics(startTime, endTime);
        
        if (metrics.isEmpty()) {
            // 如果没有历史数据，使用当前实时数据
            CacheMetrics currentMetrics = getRealTimeMetrics();
            metrics = Arrays.asList(currentMetrics);
        }
        
        // 计算总体指标
        double overallHitRate = metrics.stream()
                .mapToDouble(CacheMetrics::getHitRate)
                .average()
                .orElse(0.0);
        
        double avgResponseTime = metrics.stream()
                .mapToDouble(CacheMetrics::getAvgResponseTime)
                .average()
                .orElse(0.0);
        
        long totalRequests = metrics.stream()
                .mapToLong(CacheMetrics::getTotalRequests)
                .sum();
        
        long peakRequestsPerHour = metrics.stream()
                .mapToLong(CacheMetrics::getTotalRequests)
                .max()
                .orElse(0);
        
        // 按类型统计
        Map<String, Double> hitRateByType = calculateHitRateByType(metrics);
        Map<String, Double> avgResponseTimeByType = calculateAvgResponseTimeByType();
        
        // 生成建议
        List<String> recommendations = generateRecommendations(overallHitRate, avgResponseTime, totalRequests);
        
        return new CachePerformanceReport(
            LocalDateTime.now(),
            hours,
            overallHitRate,
            avgResponseTime,
            totalRequests,
            peakRequestsPerHour,
            hitRateByType,
            avgResponseTimeByType,
            recommendations
        );
    }

    @Override
    public List<CacheHotspot> getCacheHotspots(int topN) {
        List<ModelTask> hotTasks = taskRepository.findHotCacheTasks(PageRequest.of(0, topN)).getContent();
        
        return hotTasks.stream()
                .map(task -> new CacheHotspot(
                    task.getTaskId(),
                    task.getInputHash(),
                    task.getType().name(),
                    task.getCacheHitCount() != null ? task.getCacheHitCount() : 0,
                    calculateTaskHitRate(task),
                    task.getLastAccessedAt(),
                    calculateTaskAvgResponseTime(task)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public CacheTrendAnalysis getTrendAnalysis(int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        List<CacheMetrics> metrics = getHistoricalMetrics(startTime, endTime);
        
        if (metrics.size() < 2) {
            return new CacheTrendAnalysis(
                LocalDateTime.now(),
                days,
                0.0, 0.0, 0.0,
                new HashMap<>(),
                Arrays.asList("数据不足，无法进行趋势分析")
            );
        }
        
        // 计算趋势
        double hitRateTrend = calculateTrend(metrics, CacheMetrics::getHitRate);
        double responseTimeTrend = calculateTrend(metrics, CacheMetrics::getAvgResponseTime);
        double requestVolumeTrend = calculateTrend(metrics, m -> (double) m.getTotalRequests());
        
        // 按类型趋势分析
        Map<String, Double> trendsByType = calculateTrendsByType(metrics);
        
        // 生成洞察
        List<String> insights = generateInsights(hitRateTrend, responseTimeTrend, requestVolumeTrend);
        
        return new CacheTrendAnalysis(
            LocalDateTime.now(),
            days,
            hitRateTrend,
            responseTimeTrend,
            requestVolumeTrend,
            trendsByType,
            insights
        );
    }

    @Override
    public void resetMetrics() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        hitsByType.clear();
        operationCounts.clear();
        responseTimes.clear();
        historicalMetrics.clear();
        
        logger.info("Cache metrics have been reset");
    }

    // 私有辅助方法

    private double calculateAverageResponseTime() {
        return responseTimes.values().stream()
                .flatMap(List::stream)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    private long estimateMemoryUsage() {
        // 简化的内存使用估算
        long cachedTasks = taskRepository.countByCachedTrue();
        return cachedTasks * 1024; // 假设每个缓存任务占用1KB
    }

    private Map<String, Double> calculateHitRateByType(List<CacheMetrics> metrics) {
        Map<String, Double> result = new HashMap<>();
        
        for (String type : hitsByType.keySet()) {
            double avgHitRate = metrics.stream()
                    .filter(m -> m.getHitsByType() != null && m.getHitsByType().containsKey(type))
                    .mapToDouble(m -> {
                        long typeHits = m.getHitsByType().get(type);
                        long totalHits = m.getCacheHits();
                        return totalHits > 0 ? (double) typeHits / totalHits : 0.0;
                    })
                    .average()
                    .orElse(0.0);
            result.put(type, avgHitRate);
        }
        
        return result;
    }

    private Map<String, Double> calculateAvgResponseTimeByType() {
        Map<String, Double> result = new HashMap<>();
        
        for (Map.Entry<String, List<Long>> entry : responseTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            double avgTime = times.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            result.put(key, avgTime);
        }
        
        return result;
    }

    private List<String> generateRecommendations(double hitRate, double avgResponseTime, long totalRequests) {
        List<String> recommendations = new ArrayList<>();
        
        if (hitRate < 0.5) {
            recommendations.add("缓存命中率较低（" + String.format("%.1f%%", hitRate * 100) + 
                             "），建议优化缓存策略或增加预热");
        }
        
        if (avgResponseTime > 1000) {
            recommendations.add("平均响应时间较高（" + String.format("%.0fms", avgResponseTime) + 
                             "），建议优化缓存存储或网络配置");
        }
        
        if (totalRequests > 10000) {
            recommendations.add("请求量较大（" + totalRequests + "），建议考虑增加缓存容量");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("缓存性能良好，继续保持当前配置");
        }
        
        return recommendations;
    }

    private double calculateTaskHitRate(ModelTask task) {
        Integer hitCount = task.getCacheHitCount();
        Integer accessCount = task.getAccessCount();
        
        if (hitCount == null || accessCount == null || accessCount == 0) {
            return 0.0;
        }
        
        return (double) hitCount / accessCount;
    }

    private double calculateTaskAvgResponseTime(ModelTask task) {
        // 简化实现，实际应该从历史记录中计算
        return 100.0; // 默认100ms
    }

    private double calculateTrend(List<CacheMetrics> metrics, java.util.function.Function<CacheMetrics, Double> valueExtractor) {
        if (metrics.size() < 2) {
            return 0.0;
        }
        
        List<Double> values = metrics.stream()
                .map(valueExtractor)
                .collect(Collectors.toList());
        
        // 简单的线性趋势计算
        double firstValue = values.get(0);
        double lastValue = values.get(values.size() - 1);
        
        return lastValue - firstValue;
    }

    private Map<String, Double> calculateTrendsByType(List<CacheMetrics> metrics) {
        Map<String, Double> trends = new HashMap<>();
        
        for (String type : hitsByType.keySet()) {
            List<Double> typeValues = metrics.stream()
                    .filter(m -> m.getHitsByType() != null && m.getHitsByType().containsKey(type))
                    .map(m -> (double) m.getHitsByType().get(type))
                    .collect(Collectors.toList());
            
            if (typeValues.size() >= 2) {
                double trend = typeValues.get(typeValues.size() - 1) - typeValues.get(0);
                trends.put(type, trend);
            }
        }
        
        return trends;
    }

    private List<String> generateInsights(double hitRateTrend, double responseTimeTrend, double requestVolumeTrend) {
        List<String> insights = new ArrayList<>();
        
        if (hitRateTrend > 0.05) {
            insights.add("缓存命中率呈上升趋势，缓存策略效果良好");
        } else if (hitRateTrend < -0.05) {
            insights.add("缓存命中率呈下降趋势，需要关注缓存策略调整");
        }
        
        if (responseTimeTrend > 100) {
            insights.add("响应时间呈上升趋势，可能需要优化系统性能");
        } else if (responseTimeTrend < -100) {
            insights.add("响应时间呈下降趋势，系统性能有所改善");
        }
        
        if (requestVolumeTrend > 1000) {
            insights.add("请求量呈上升趋势，建议关注系统容量规划");
        }
        
        if (insights.isEmpty()) {
            insights.add("各项指标相对稳定，系统运行正常");
        }
        
        return insights;
    }

    /**
     * 定期保存当前指标到历史记录
     * 应该通过定时任务调用
     */
    public void saveCurrentMetricsToHistory() {
        CacheMetrics currentMetrics = getRealTimeMetrics();
        historicalMetrics.add(currentMetrics);
        
        // 保持历史记录在合理范围内（保留最近7天的数据）
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        historicalMetrics.removeIf(m -> m.getTimestamp().isBefore(cutoffTime));
        
        logger.debug("Saved current metrics to history, total historical records: {}", 
                    historicalMetrics.size());
    }
}