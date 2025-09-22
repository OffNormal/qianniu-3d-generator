package org.example.infrastructure.repository;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.repository.ICacheRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存仓储实现（内存实现）
 * 
 * @author Assistant
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Repository
public class CacheRepositoryImpl implements ICacheRepository {
    
    // 使用内存存储缓存项
    private final Map<String, CacheItemEntity> cacheStore = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    @Override
    public CacheItemEntity findByCacheKey(String cacheKey) {
        CacheItemEntity item = cacheStore.get(cacheKey);
        if (item != null) {
            // 检查是否过期
            if (item.getExpireTime() != null && item.getExpireTime().isBefore(LocalDateTime.now())) {
                cacheStore.remove(cacheKey);
                missCount.incrementAndGet();
                return null;
            }
            hitCount.incrementAndGet();
            return item;
        }
        missCount.incrementAndGet();
        return null;
    }
    
    @Override
    public void save(CacheItemEntity cacheItem) {
        if (cacheItem != null && cacheItem.getCacheKey() != null) {
            cacheStore.put(cacheItem.getCacheKey(), cacheItem);
            log.debug("缓存项已保存: {}", cacheItem.getCacheKey());
        }
    }
    
    @Override
    public List<CacheItemEntity> findSimilarByContent(String inputContent, double threshold) {
        List<CacheItemEntity> similarItems = new ArrayList<>();
        
        for (CacheItemEntity item : cacheStore.values()) {
            // 检查是否过期
            if (item.getExpireTime() != null && item.getExpireTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            
            // 简单的相似度计算（基于关键词匹配）
            double similarity = calculateSimilarity(inputContent, item.getInputContent());
            if (similarity >= threshold) {
                similarItems.add(item);
            }
        }
        
        return similarItems;
    }
    
    @Override
    public int deleteExpired(LocalDateTime expireTime) {
        int deletedCount = 0;
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, CacheItemEntity> entry : cacheStore.entrySet()) {
            CacheItemEntity item = entry.getValue();
            if (item.getExpireTime() != null && item.getExpireTime().isBefore(expireTime)) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        for (String key : expiredKeys) {
            cacheStore.remove(key);
            deletedCount++;
        }
        
        log.info("删除过期缓存项数量: {}", deletedCount);
        return deletedCount;
    }
    
    @Override
    public long count() {
        return cacheStore.size();
    }
    
    @Override
    public long getTotalHitCount() {
        return hitCount.get() + missCount.get();
    }
    
    @Override
    public long getHitCount() {
        return hitCount.get();
    }
    
    @Override
    public long getMissCount() {
        return missCount.get();
    }
    
    @Override
    public long getTotalSize() {
        // 估算缓存总大小（字节）
        long totalSize = 0;
        for (CacheItemEntity item : cacheStore.values()) {
            if (item.getInputContent() != null) {
                totalSize += item.getInputContent().length() * 2; // 假设每个字符2字节
            }
            if (item.getResultUrl() != null) {
                totalSize += item.getResultUrl().length() * 2;
            }
            if (item.getModelFilePath() != null) {
                totalSize += item.getModelFilePath().length() * 2;
            }
        }
        return totalSize;
    }
    
    /**
     * 计算两个字符串的相似度
     * 简单实现：基于公共子串长度
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        // 转换为小写并去除空格
        str1 = str1.toLowerCase().trim();
        str2 = str2.toLowerCase().trim();
        
        // 计算最长公共子序列长度
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int commonLength = longestCommonSubsequence(str1, str2);
        return (double) commonLength / maxLength;
    }
    
    /**
     * 计算最长公共子序列长度
     */
    private int longestCommonSubsequence(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
}