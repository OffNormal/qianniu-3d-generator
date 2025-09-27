package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelTaskRepository;
import com.qiniu.model3d.service.CacheEvictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存淘汰策略服务实现
 * 实现基于LRU、访问频率和存储空间的智能缓存清理
 */
@Service
public class CacheEvictionServiceImpl implements CacheEvictionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionServiceImpl.class);
    
    @Autowired
    private ModelTaskRepository modelTaskRepository;
    
    // 缓存配置参数
    @Value("${cache.eviction.max-cache-size:10737418240}") // 10GB 默认
    private long maxCacheSize;
    
    @Value("${cache.eviction.max-cache-count:1000}")
    private int maxCacheCount;
    
    @Value("${cache.eviction.cleanup-threshold:0.8}")
    private double cleanupThreshold;
    
    @Value("${cache.eviction.cleanup-target:0.7}")
    private double cleanupTarget;
    
    @Value("${cache.eviction.max-age-days:30}")
    private int maxAgeDays;
    
    @Value("${cache.eviction.min-access-count:1}")
    private int minAccessCount;
    
    @Value("${app.file.model-dir}")
    private String modelDir;
    
    // 权重配置
    @Value("${cache.eviction.weight.age:0.3}")
    private double ageWeight;
    
    @Value("${cache.eviction.weight.access-frequency:0.4}")
    private double accessFrequencyWeight;
    
    @Value("${cache.eviction.weight.file-size:0.2}")
    private double fileSizeWeight;
    
    @Value("${cache.eviction.weight.similarity-usage:0.1}")
    private double similarityUsageWeight;
    
    /**
     * 定时执行缓存清理 - 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void scheduledCacheCleanup() {
        if (shouldPerformCleanup()) {
            logger.info("开始定时缓存清理");
            performCacheCleanup();
        }
    }
    
    @Override
    public void performCacheCleanup() {
        try {
            CacheStatistics stats = getCacheStatistics();
            logger.info("缓存清理前统计: 总大小={}MB, 总数量={}, 命中率={}", 
                       stats.getTotalCacheSize() / 1024 / 1024, 
                       stats.getTotalCacheCount(), 
                       stats.getCacheHitRate());
            
            if (!shouldPerformCleanup()) {
                logger.info("缓存使用量未达到清理阈值，跳过清理");
                return;
            }
            
            // 计算需要清理的数量
            long currentSize = stats.getTotalCacheSize();
            long targetSize = (long) (maxCacheSize * cleanupTarget);
            
            if (currentSize > targetSize) {
                List<ModelTask> candidates = getCandidatesForEviction(Integer.MAX_VALUE);
                long cleanedSize = 0;
                int cleanedCount = 0;
                
                for (ModelTask task : candidates) {
                    if (currentSize - cleanedSize <= targetSize) {
                        break;
                    }
                    
                    long taskSize = calculateTaskFileSize(task);
                    if (evictTask(task)) {
                        cleanedSize += taskSize;
                        cleanedCount++;
                    }
                }
                
                logger.info("缓存清理完成: 清理了{}个任务, 释放了{}MB空间", 
                           cleanedCount, cleanedSize / 1024 / 1024);
            }
            
        } catch (Exception e) {
            logger.error("缓存清理过程中发生错误", e);
        }
    }
    
    @Override
    public int forceEvictCache(int count) {
        List<ModelTask> candidates = getCandidatesForEviction(count);
        int evictedCount = 0;
        
        for (ModelTask task : candidates) {
            if (evictTask(task)) {
                evictedCount++;
            }
        }
        
        logger.info("强制清理缓存: 请求清理{}个, 实际清理{}个", count, evictedCount);
        return evictedCount;
    }
    
    @Override
    public List<ModelTask> getCandidatesForEviction(int maxCount) {
        // 获取所有已完成且有缓存文件的任务
        List<ModelTask> cachedTasks = modelTaskRepository.findByStatusAndCachedTrue(
            ModelTask.TaskStatus.COMPLETED
        );
        
        // 计算每个任务的缓存价值分数并排序
        return cachedTasks.stream()
            .filter(this::hasValidCacheFiles)
            .sorted((t1, t2) -> Double.compare(calculateCacheValue(t1), calculateCacheValue(t2)))
            .limit(maxCount)
            .collect(Collectors.toList());
    }
    
    @Override
    public double calculateCacheValue(ModelTask task) {
        double score = 0.0;
        
        // 1. 年龄因子 (越老分数越低)
        long ageInDays = ChronoUnit.DAYS.between(task.getCreatedAt(), LocalDateTime.now());
        double ageScore = Math.max(0, 1.0 - (double) ageInDays / maxAgeDays);
        score += ageScore * ageWeight;
        
        // 2. 访问频率因子
        int accessCount = task.getAccessCount() != null ? task.getAccessCount() : 0;
        double accessScore = Math.min(1.0, (double) accessCount / 10.0); // 10次访问为满分
        score += accessScore * accessFrequencyWeight;
        
        // 3. 文件大小因子 (文件越大分数越低，优先清理大文件)
        long fileSize = calculateTaskFileSize(task);
        double sizeScore = Math.max(0, 1.0 - (double) fileSize / (100 * 1024 * 1024)); // 100MB为基准
        score += sizeScore * fileSizeWeight;
        
        // 4. 相似度使用因子
        int similarityUsage = task.getSimilarityUsageCount() != null ? task.getSimilarityUsageCount() : 0;
        double similarityScore = Math.min(1.0, (double) similarityUsage / 5.0); // 5次相似度匹配为满分
        score += similarityScore * similarityUsageWeight;
        
        // 5. 最后访问时间因子
        if (task.getLastAccessedAt() != null) {
            long daysSinceLastAccess = ChronoUnit.DAYS.between(task.getLastAccessedAt(), LocalDateTime.now());
            double lastAccessScore = Math.max(0, 1.0 - (double) daysSinceLastAccess / 7.0); // 7天为基准
            score += lastAccessScore * 0.1; // 额外的小权重
        }
        
        return score;
    }
    
    @Override
    public boolean shouldPerformCleanup() {
        CacheStatistics stats = getCacheStatistics();
        
        // 检查大小阈值
        if (stats.getTotalCacheSize() > maxCacheSize * cleanupThreshold) {
            return true;
        }
        
        // 检查数量阈值
        if (stats.getTotalCacheCount() > maxCacheCount * cleanupThreshold) {
            return true;
        }
        
        // 检查磁盘空间
        try {
            Path modelPath = Paths.get(modelDir);
            long freeSpace = Files.getFileStore(modelPath).getUsableSpace();
            long totalSpace = Files.getFileStore(modelPath).getTotalSpace();
            double freeSpaceRatio = (double) freeSpace / totalSpace;
            
            if (freeSpaceRatio < 0.1) { // 可用空间少于10%
                return true;
            }
        } catch (IOException e) {
            logger.warn("无法检查磁盘空间", e);
        }
        
        return false;
    }
    
    @Override
    public CacheStatistics getCacheStatistics() {
        List<ModelTask> cachedTasks = modelTaskRepository.findByStatusAndCachedTrue(
            ModelTask.TaskStatus.COMPLETED
        );
        
        long totalSize = cachedTasks.stream()
            .mapToLong(this::calculateTaskFileSize)
            .sum();
        
        long totalCount = cachedTasks.size();
        
        // 计算命中率
        long totalAccess = cachedTasks.stream()
            .mapToLong(task -> task.getAccessCount() != null ? task.getAccessCount() : 0)
            .sum();
        
        long cacheHits = cachedTasks.stream()
            .mapToLong(task -> task.getCacheHitCount() != null ? task.getCacheHitCount() : 0)
            .sum();
        
        double hitRate = totalAccess > 0 ? (double) cacheHits / totalAccess : 0.0;
        
        // 获取最老缓存的年龄
        long oldestAge = cachedTasks.stream()
            .mapToLong(task -> ChronoUnit.DAYS.between(task.getCreatedAt(), LocalDateTime.now()))
            .max()
            .orElse(0);
        
        // 获取可用空间
        long availableSpace = 0;
        try {
            Path modelPath = Paths.get(modelDir);
            availableSpace = Files.getFileStore(modelPath).getUsableSpace();
        } catch (IOException e) {
            logger.warn("无法获取可用磁盘空间", e);
        }
        
        return new CacheStatistics(totalSize, totalCount, availableSpace, hitRate, oldestAge);
    }
    
    @Override
    @Transactional
    public boolean forceEvict(String taskId) {
        try {
            Optional<ModelTask> taskOpt = modelTaskRepository.findByTaskId(taskId);
            if (taskOpt.isEmpty()) {
                logger.warn("任务不存在: taskId={}", taskId);
                return false;
            }
            
            ModelTask task = taskOpt.get();
            if (!Boolean.TRUE.equals(task.getCached())) {
                logger.warn("任务未缓存: taskId={}", taskId);
                return false;
            }
            
            return evictTask(task);
            
        } catch (Exception e) {
            logger.error("强制淘汰任务失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    /**
     * 淘汰指定任务的缓存
     */
    @Transactional
    private boolean evictTask(ModelTask task) {
        try {
            // 删除物理文件
            deleteTaskFiles(task);
            
            // 更新数据库记录
            task.setCached(false);
            task.setFileSignature(null);
            modelTaskRepository.save(task);
            
            logger.debug("成功淘汰缓存任务: taskId={}", task.getTaskId());
            return true;
            
        } catch (Exception e) {
            logger.error("淘汰缓存任务失败: taskId={}", task.getTaskId(), e);
            return false;
        }
    }
    
    /**
     * 删除任务相关的所有文件
     */
    private void deleteTaskFiles(ModelTask task) {
        try {
            // 删除模型文件
            if (task.getObjFilePath() != null) {
                deleteFileIfExists(task.getObjFilePath());
            }
            if (task.getGltfFilePath() != null) {
                deleteFileIfExists(task.getGltfFilePath());
            }
            if (task.getStlFilePath() != null) {
                deleteFileIfExists(task.getStlFilePath());
            }
            
            // 删除预览图片
            if (task.getPreviewImagePath() != null) {
                deleteFileIfExists(task.getPreviewImagePath());
            }
            
        } catch (Exception e) {
            logger.warn("删除任务文件时发生错误: taskId={}", task.getTaskId(), e);
        }
    }
    
    /**
     * 删除文件（如果存在）
     */
    private void deleteFileIfExists(String filePath) {
        if (filePath != null && !filePath.trim().isEmpty()) {
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                logger.warn("删除文件失败: {}", filePath, e);
            }
        }
    }
    
    /**
     * 计算任务文件的总大小
     */
    private long calculateTaskFileSize(ModelTask task) {
        long totalSize = 0;
        
        totalSize += getFileSize(task.getObjFilePath());
        totalSize += getFileSize(task.getGltfFilePath());
        totalSize += getFileSize(task.getStlFilePath());
        totalSize += getFileSize(task.getPreviewImagePath());
        
        return totalSize;
    }
    
    /**
     * 获取文件大小
     */
    private long getFileSize(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return 0;
        }
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (IOException e) {
            logger.debug("获取文件大小失败: {}", filePath);
        }
        
        return 0;
    }
    
    /**
     * 检查任务是否有有效的缓存文件
     */
    private boolean hasValidCacheFiles(ModelTask task) {
        if (!Boolean.TRUE.equals(task.getCached())) {
            return false;
        }
        
        // 至少要有一个模型文件存在
        return (task.getObjFilePath() != null && Files.exists(Paths.get(task.getObjFilePath()))) ||
               (task.getGltfFilePath() != null && Files.exists(Paths.get(task.getGltfFilePath()))) ||
               (task.getStlFilePath() != null && Files.exists(Paths.get(task.getStlFilePath())));
    }
}