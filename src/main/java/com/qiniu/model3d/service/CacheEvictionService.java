package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelTask;
import java.util.List;

/**
 * 缓存淘汰策略服务接口
 * 负责管理缓存的清理和淘汰策略
 */
public interface CacheEvictionService {
    
    /**
     * 检查并执行缓存清理
     * 根据配置的策略自动清理过期或低价值的缓存
     */
    void performCacheCleanup();
    
    /**
     * 强制清理指定数量的缓存项
     * @param count 需要清理的缓存项数量
     * @return 实际清理的缓存项数量
     */
    int forceEvictCache(int count);
    
    /**
     * 获取建议清理的缓存任务列表
     * @param maxCount 最大返回数量
     * @return 建议清理的任务列表，按优先级排序
     */
    List<ModelTask> getCandidatesForEviction(int maxCount);
    
    /**
     * 计算任务的缓存价值分数
     * 分数越低，越适合被淘汰
     * @param task 模型任务
     * @return 缓存价值分数
     */
    double calculateCacheValue(ModelTask task);
    
    /**
     * 检查是否需要进行缓存清理
     * @return true 如果需要清理
     */
    boolean shouldPerformCleanup();
    
    /**
     * 获取当前缓存使用统计
     * @return 缓存统计信息
     */
    CacheStatistics getCacheStatistics();
    
    /**
     * 强制淘汰指定任务的缓存
     * @param taskId 任务ID
     * @return 是否成功淘汰
     */
    boolean forceEvict(String taskId);
    
    /**
     * 缓存统计信息内部类
     */
    class CacheStatistics {
        private long totalCacheSize;
        private long totalCacheCount;
        private long availableSpace;
        private double cacheHitRate;
        private long oldestCacheAge;
        
        // 构造函数
        public CacheStatistics(long totalCacheSize, long totalCacheCount, 
                             long availableSpace, double cacheHitRate, long oldestCacheAge) {
            this.totalCacheSize = totalCacheSize;
            this.totalCacheCount = totalCacheCount;
            this.availableSpace = availableSpace;
            this.cacheHitRate = cacheHitRate;
            this.oldestCacheAge = oldestCacheAge;
        }
        
        // Getters
        public long getTotalCacheSize() { return totalCacheSize; }
        public long getTotalCacheCount() { return totalCacheCount; }
        public long getAvailableSpace() { return availableSpace; }
        public double getCacheHitRate() { return cacheHitRate; }
        public long getOldestCacheAge() { return oldestCacheAge; }
    }
}