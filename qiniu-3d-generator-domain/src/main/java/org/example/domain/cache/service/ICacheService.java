package org.example.domain.cache.service;

import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;

/**
 * 缓存服务接口
 */
public interface ICacheService {
    
    /**
     * 查找缓存
     * @param request 生成请求
     * @return 缓存项，如果没有则返回null
     */
    CacheItemEntity findCache(GenerationRequest request);
    
    /**
     * 保存缓存
     * @param cacheItem 缓存项
     */
    void saveCache(CacheItemEntity cacheItem);
    
    /**
     * 生成缓存键
     * @param request 生成请求
     * @return 缓存键
     */
    String generateCacheKey(GenerationRequest request);
    
    /**
     * 计算输入哈希
     * @param inputContent 输入内容
     * @return 哈希值
     */
    String calculateInputHash(String inputContent);
    
    /**
     * 查找相似缓存
     * @param inputContent 输入内容
     * @param threshold 相似度阈值
     * @return 相似缓存列表
     */
    java.util.List<CacheItemEntity> findSimilarCache(String inputContent, double threshold);
    
    /**
     * 清理过期缓存
     * @return 清理的缓存数量
     */
    int cleanExpiredCache();
}