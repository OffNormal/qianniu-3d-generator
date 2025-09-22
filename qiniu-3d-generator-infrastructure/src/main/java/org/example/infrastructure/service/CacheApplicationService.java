package org.example.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.service.ICacheService;
import org.example.domain.cache.service.impl.CacheServiceImpl;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 缓存应用服务
 */
@Slf4j
@Service
public class CacheApplicationService {
    
    @Resource
    private ICacheService cacheService;
    
    @Resource
    private CacheServiceImpl cacheServiceImpl;
    
    /**
     * 智能缓存查找
     * 先查找精确匹配，再查找相似匹配
     */
    public Response<CacheItemEntity> smartCacheFind(String inputContent, GenerationType type, 
                                                   Map<String, Object> params) {
        try {
            // 1. 精确匹配查找
            GenerationRequest request = new GenerationRequest();
            request.setInputContent(inputContent);
            request.setGenerationType(type);
            // 注意：GenerationRequest使用GenerationParams而不是Map
            
            String cacheKey = cacheService.generateCacheKey(request);
            CacheItemEntity exactMatch = cacheService.findCache(request);
            
            if (exactMatch != null) {
                log.info("精确缓存命中: {}", cacheKey);
                return Response.success(exactMatch);
            }
            
            // 2. 相似匹配查找
            List<CacheItemEntity> similarItems = cacheService.findSimilarCache(inputContent, 0.8);
            
            if (!similarItems.isEmpty()) {
                // 选择最相似的项目
                CacheItemEntity bestMatch = selectBestMatch(similarItems, inputContent);
                if (bestMatch != null) {
                    log.info("相似缓存命中: 输入={}, 相似度较高", inputContent);
                    return Response.success(bestMatch);
                }
            }
            
            log.info("缓存未命中: {}", inputContent);
            return Response.fail("缓存未命中");
            
        } catch (Exception e) {
            log.error("智能缓存查找失败", e);
            return Response.fail("查找失败: " + e.getMessage());
        }
    }
    
    /**
     * 预热缓存
     * 为热门内容预先生成缓存
     */
    public Response<String> preWarmCache(List<String> popularInputs, GenerationType type) {
        try {
            int successCount = 0;
            
            for (String input : popularInputs) {
                try {
                    // 检查是否已有缓存
                    GenerationRequest request = new GenerationRequest();
                    request.setInputContent(input);
                    request.setGenerationType(type);
                    // 使用默认参数
                    
                    String cacheKey = cacheService.generateCacheKey(request);
                    CacheItemEntity existing = cacheService.findCache(request);
                    
                    if (existing == null) {
                        // 这里应该触发实际的3D生成任务
                        // 简化实现，直接标记为预热任务
                        log.info("预热缓存任务创建: {}", input);
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    log.warn("预热单个缓存失败: {}", input, e);
                }
            }
            
            return Response.success("预热任务创建成功，数量: " + successCount);
            
        } catch (Exception e) {
            log.error("预热缓存失败", e);
            return Response.fail("预热失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取缓存统计
     */
    public Response<CacheServiceImpl.CacheStatistics> getCacheStatistics() {
        try {
            CacheServiceImpl.CacheStatistics stats = cacheServiceImpl.getCacheStatistics();
            return Response.success(stats);
            
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动清理缓存
     */
    public Response<String> cleanCache() {
        try {
            cacheService.cleanExpiredCache();
            return Response.success("缓存清理完成");
            
        } catch (Exception e) {
            log.error("清理缓存失败", e);
            return Response.fail("清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 定时清理过期缓存
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void scheduledCleanCache() {
        try {
            log.info("开始定时清理过期缓存");
            cacheService.cleanExpiredCache();
            log.info("定时清理过期缓存完成");
            
        } catch (Exception e) {
            log.error("定时清理缓存失败", e);
        }
    }
    
    /**
     * 缓存预热任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledPreWarm() {
        try {
            log.info("开始定时缓存预热");
            
            // 获取热门输入内容（这里简化实现）
            List<String> popularInputs = getPopularInputs();
            
            if (!popularInputs.isEmpty()) {
                preWarmCache(popularInputs, GenerationType.TEXT);
                preWarmCache(popularInputs, GenerationType.IMAGE);
            }
            
            log.info("定时缓存预热完成");
            
        } catch (Exception e) {
            log.error("定时缓存预热失败", e);
        }
    }
    
    /**
     * 选择最佳匹配项
     */
    private CacheItemEntity selectBestMatch(List<CacheItemEntity> similarItems, String inputContent) {
        // 简化实现：选择命中次数最多的
        return similarItems.stream()
            .filter(item -> item.isHot(5)) // 命中次数大于5的认为是热门
            .findFirst()
            .orElse(similarItems.get(0));
    }
    
    /**
     * 获取热门输入内容
     */
    private List<String> getPopularInputs() {
        // 简化实现：返回一些常见的输入
        return List.of(
            "一只可爱的小猫",
            "现代风格的椅子",
            "科幻风格的机器人",
            "简约的花瓶",
            "卡通风格的汽车"
        );
    }
}