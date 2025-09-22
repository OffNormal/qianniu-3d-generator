package org.example.trigger.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.service.impl.CacheServiceImpl;
import org.example.infrastructure.service.CacheApplicationService;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 缓存优化控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@Tag(name = "缓存优化", description = "缓存优化相关接口")
public class CacheController {
    
    @Resource
    private CacheApplicationService cacheApplicationService;
    
    /**
     * 智能缓存查找
     */
    @PostMapping("/smart-find")
    @Operation(summary = "智能缓存查找", description = "根据输入内容智能查找缓存")
    public Response<CacheItemEntity> smartCacheFind(
            @Parameter(description = "输入内容") @RequestParam String inputContent,
            @Parameter(description = "生成类型") @RequestParam GenerationType type,
            @Parameter(description = "参数") @RequestBody(required = false) Map<String, Object> params) {
        
        return cacheApplicationService.smartCacheFind(inputContent, type, params);
    }
    
    /**
     * 预热缓存
     */
    @PostMapping("/pre-warm")
    @Operation(summary = "预热缓存", description = "预热热门内容的缓存")
    public Response<String> preWarmCache(
            @Parameter(description = "热门输入列表") @RequestBody List<String> popularInputs,
            @Parameter(description = "生成类型") @RequestParam GenerationType type) {
        
        return cacheApplicationService.preWarmCache(popularInputs, type);
    }
    
    /**
     * 获取缓存统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取缓存统计", description = "获取缓存的统计信息")
    public Response<CacheServiceImpl.CacheStatistics> getCacheStatistics() {
        
        return cacheApplicationService.getCacheStatistics();
    }
    
    /**
     * 清理缓存
     */
    @PostMapping("/clean")
    @Operation(summary = "清理过期缓存", description = "清理过期的缓存项")
    public Response<String> cleanCache() {
        
        return cacheApplicationService.cleanCache();
    }
    
    /**
     * 缓存命中率报告
     */
    @GetMapping("/hit-rate-report")
    @Operation(summary = "缓存命中率报告", description = "获取缓存命中率的详细报告")
    public Response<Map<String, Object>> getHitRateReport(
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") int days) {
        
        try {
            // 获取缓存统计
            Response<CacheServiceImpl.CacheStatistics> statsResponse = getCacheStatistics();
            
            if (!statsResponse.isSuccess()) {
                return Response.fail("获取统计失败");
            }
            
            CacheServiceImpl.CacheStatistics stats = statsResponse.getData();
            
            // 构建报告
            Map<String, Object> report = Map.of(
                "totalCacheItems", stats.getTotalItems(),
                "totalHits", stats.getHitCount(),
                "hitRate", stats.getHitRate(),
                "expiredItems", 0L, // 暂时设为0，需要添加过期统计
                "reportDays", days,
                "cacheEfficiency", calculateCacheEfficiency(stats),
                "recommendations", generateRecommendations(stats)
            );
            
            return Response.success(report);
            
        } catch (Exception e) {
            log.error("获取缓存命中率报告失败", e);
            return Response.fail("获取报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 缓存优化建议
     */
    @GetMapping("/optimization-suggestions")
    @Operation(summary = "缓存优化建议", description = "获取缓存优化的建议")
    public Response<List<String>> getOptimizationSuggestions() {
        
        try {
            Response<CacheServiceImpl.CacheStatistics> statsResponse = getCacheStatistics();
            
            if (!statsResponse.isSuccess()) {
                return Response.fail("获取统计失败");
            }
            
            CacheServiceImpl.CacheStatistics stats = statsResponse.getData();
            List<String> suggestions = generateOptimizationSuggestions(stats);
            
            return Response.success(suggestions);
            
        } catch (Exception e) {
            log.error("获取优化建议失败", e);
            return Response.fail("获取建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算缓存效率
     */
    private double calculateCacheEfficiency(CacheServiceImpl.CacheStatistics stats) {
        if (stats.getTotalItems() == 0) {
            return 0.0;
        }
        
        // 综合考虑命中率和过期率
        double hitRateScore = stats.getHitRate() * 100;
        double expiredRateScore = 100.0; // 暂时设为100，需要添加过期统计
        
        return (hitRateScore + expiredRateScore) / 2.0;
    }
    
    /**
     * 生成推荐建议
     */
    private List<String> generateRecommendations(CacheServiceImpl.CacheStatistics stats) {
        List<String> recommendations = List.of();
        
        if (stats.getHitRate() < 0.3) {
            recommendations = List.of("缓存命中率较低，建议增加预热策略");
        } else if (stats.getHitRate() > 0.8) {
            recommendations = List.of("缓存命中率良好，继续保持");
        } else {
            recommendations = List.of("缓存命中率中等，可考虑优化相似度匹配算法");
        }
        
        return recommendations;
    }
    
    /**
     * 生成优化建议
     */
    private List<String> generateOptimizationSuggestions(CacheServiceImpl.CacheStatistics stats) {
        List<String> suggestions = List.of();
        
        if (stats.getHitRate() < 0.5) {
            suggestions = List.of(
                "增加缓存预热频率",
                "优化缓存键生成策略",
                "实现更智能的相似度匹配",
                "增加热门内容的缓存时间"
            );
        } else {
            suggestions = List.of(
                "当前缓存策略表现良好",
                "可考虑增加缓存容量",
                "定期分析用户行为模式"
            );
        }
        
        return suggestions;
    }
}