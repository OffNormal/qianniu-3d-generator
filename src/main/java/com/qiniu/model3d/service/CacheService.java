package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.dto.CacheResult;

import java.util.List;
import java.util.Optional;

/**
 * 缓存服务接口
 * 提供模型生成结果的缓存功能，包括完全匹配和相似度匹配
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public interface CacheService {

    /**
     * 查找完全匹配的缓存
     * 
     * @param inputText 输入文本
     * @param taskType 任务类型
     * @param complexity 复杂度
     * @param outputFormat 输出格式
     * @return 匹配的缓存任务
     */
    Optional<ModelTask> findExactMatch(String inputText, ModelTask.TaskType taskType, 
                                     String complexity, String outputFormat);

    /**
     * 查找相似度匹配的缓存
     * 
     * @param inputText 输入文本
     * @param taskType 任务类型
     * @param complexity 复杂度
     * @param outputFormat 输出格式
     * @param threshold 相似度阈值
     * @return 相似度匹配结果列表
     */
    List<CacheResult> findSimilarMatches(String inputText, ModelTask.TaskType taskType,
                                       String complexity, String outputFormat, double threshold);

    /**
     * 缓存任务结果
     * 
     * @param task 要缓存的任务
     * @return 是否缓存成功
     */
    boolean cacheTask(ModelTask task);

    /**
     * 更新缓存访问信息
     * 
     * @param taskId 任务ID
     * @return 是否更新成功
     */
    boolean updateCacheAccess(String taskId);

    /**
     * 检查缓存是否有效
     * 
     * @param task 缓存任务
     * @return 是否有效
     */
    boolean isCacheValid(ModelTask task);

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    CacheStatistics getCacheStatistics();

    /**
     * 执行缓存清理
     * 
     * @return 清理的任务数量
     */
    int performCacheCleanup();

    /**
     * 预热缓存
     * 
     * @return 预热的任务数量
     */
    int warmUpCache();

    /**
     * 强制清理缓存
     * 
     * @return 清理的任务数量
     */
    int forceCacheCleanup();

    /**
     * 获取缓存淘汰统计信息
     * 
     * @return 缓存淘汰统计信息
     */
    com.qiniu.model3d.service.CacheEvictionService.CacheStatistics getCacheEvictionStatistics();

    /**
     * 检查是否需要缓存清理
     * 
     * @return 是否需要清理
     */
    boolean shouldPerformCleanup();

    /**
     * 获取缓存淘汰候选
     * 
     * @param limit 限制数量
     * @return 淘汰候选列表
     */
    List<ModelTask> getEvictionCandidates(int limit);

    /**
     * 强制淘汰指定任务
     * 
     * @param taskId 任务ID
     * @return 是否成功淘汰
     */
    boolean forceEvictTask(String taskId);

    /**
     * 执行智能缓存预热
     * 
     * @return 预热的任务数量
     */
    int performIntelligentWarmup();

    /**
     * 获取预热统计信息
     * 
     * @return 预热统计信息
     */
    com.qiniu.model3d.service.CacheWarmupService.WarmupStatistics getWarmupStatistics();

    /**
     * 计算输入哈希值
     * 
     * @param inputText 输入文本
     * @param taskType 任务类型
     * @param complexity 复杂度
     * @param outputFormat 输出格式
     * @return 输入哈希值
     */
    String calculateInputHash(String inputText, ModelTask.TaskType taskType, 
                            String complexity, String outputFormat);

    /**
     * 计算文件签名
     * 
     * @param filePath 文件路径
     * @return 文件签名
     */
    String calculateFileSignature(String filePath);

    /**
     * 更新相似度使用计数
     * 
     * @param taskId 任务ID
     * @return 是否更新成功
     */
    boolean updateSimilarityUsage(String taskId);

    /**
     * 缓存统计信息
     */
    class CacheStatistics {
        private long totalCachedTasks;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long storageSize;

        // 构造函数
        public CacheStatistics(long totalCachedTasks, long hitCount, long missCount, long storageSize) {
            this.totalCachedTasks = totalCachedTasks;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitCount + missCount > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
            this.storageSize = storageSize;
        }

        // Getters
        public long getTotalCachedTasks() { return totalCachedTasks; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getStorageSize() { return storageSize; }
    }
}