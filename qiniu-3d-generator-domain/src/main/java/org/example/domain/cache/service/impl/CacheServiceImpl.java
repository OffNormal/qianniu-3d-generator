package org.example.domain.cache.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.repository.ICacheRepository;
import org.example.domain.cache.service.ICacheService;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存服务实现
 * 
 * @author Assistant
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class CacheServiceImpl implements ICacheService {

    private final ICacheRepository cacheRepository;
    private static final String CACHE_PREFIX = "3d_model_cache:";

    public CacheServiceImpl(ICacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @Override
    public CacheItemEntity findCache(GenerationRequest request) {
        try {
            String cacheKey = generateCacheKey(request);
            return cacheRepository.findByCacheKey(cacheKey);
        } catch (Exception e) {
            log.error("查找缓存失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void saveCache(CacheItemEntity cacheItem) {
        try {
            cacheRepository.save(cacheItem);
            log.info("缓存保存成功: {}", cacheItem.getCacheKey());
        } catch (Exception e) {
            log.error("保存缓存失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public String generateCacheKey(GenerationRequest request) {
        try {
            String input = request.getInputContent() + ":" + request.getGenerationType().name();
            String hash = calculateInputHash(input);
            return CACHE_PREFIX + hash;
        } catch (Exception e) {
            log.error("生成缓存键失败: {}", e.getMessage(), e);
            return CACHE_PREFIX + "error:" + System.currentTimeMillis();
        }
    }

    @Override
    public String calculateInputHash(String inputContent) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(inputContent.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算输入哈希失败: {}", e.getMessage(), e);
            return String.valueOf(inputContent.hashCode());
        }
    }

    @Override
    public List<CacheItemEntity> findSimilarCache(String inputContent, double threshold) {
        try {
            // 简化实现：基于关键词匹配
            return cacheRepository.findSimilarByContent(inputContent, threshold);
        } catch (Exception e) {
            log.error("查找相似缓存失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public int cleanExpiredCache() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int count = cacheRepository.deleteExpired(now);
            log.info("过期缓存清理完成，清理数量: {}", count);
            return count;
        } catch (Exception e) {
            log.error("清理过期缓存失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getCacheStatistics() {
        try {
            long totalItems = cacheRepository.count();
            long hitCount = cacheRepository.getHitCount();
            long missCount = cacheRepository.getMissCount();
            double hitRate = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
            long totalSize = cacheRepository.getTotalSize();
            
            return new CacheStatistics(totalItems, hitCount, missCount, hitRate, totalSize);
        } catch (Exception e) {
            log.error("获取缓存统计失败: {}", e.getMessage(), e);
            return new CacheStatistics(0, 0, 0, 0.0, 0);
        }
    }

    /**
     * 计算输入内容的哈希值
     */
    private String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算哈希失败: {}", e.getMessage(), e);
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private final long totalItems;
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long totalSize;

        public CacheStatistics(long totalItems, long hitCount, long missCount, double hitRate, long totalSize) {
            this.totalItems = totalItems;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.totalSize = totalSize;
        }

        public long getTotalItems() { return totalItems; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getTotalSize() { return totalSize; }
    }
}