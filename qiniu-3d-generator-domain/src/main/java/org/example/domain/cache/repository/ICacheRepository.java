package org.example.domain.cache.repository;

import org.example.domain.cache.model.entity.CacheItemEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存仓储接口
 * 
 * @author Assistant
 * @version 1.0
 * @since 2024-01-01
 */
public interface ICacheRepository {
    
    /**
     * 根据缓存键查找缓存项
     * @param cacheKey 缓存键
     * @return 缓存项，如果没有则返回null
     */
    CacheItemEntity findByCacheKey(String cacheKey);
    
    /**
     * 保存缓存项
     * @param cacheItem 缓存项
     */
    void save(CacheItemEntity cacheItem);
    
    /**
     * 查找相似缓存
     * @param inputContent 输入内容
     * @param threshold 相似度阈值
     * @return 相似缓存列表
     */
    List<CacheItemEntity> findSimilarByContent(String inputContent, double threshold);
    
    /**
     * 删除过期缓存
     * @param expireTime 过期时间
     * @return 删除的缓存数量
     */
    int deleteExpired(LocalDateTime expireTime);
    
    /**
     * 获取缓存总数
     * @return 缓存总数
     */
    long count();
    
    /**
     * 获取总命中次数
     * @return 总命中次数
     */
    long getTotalHitCount();
    
    /**
     * 获取命中次数
     * @return 命中次数
     */
    long getHitCount();
    
    /**
     * 获取未命中次数
     * @return 未命中次数
     */
    long getMissCount();
    
    /**
     * 获取缓存总大小
     * @return 缓存总大小（字节）
     */
    long getTotalSize();
}