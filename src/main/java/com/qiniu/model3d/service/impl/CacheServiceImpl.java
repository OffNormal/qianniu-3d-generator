package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.dto.CacheResult;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelTaskRepository;
import com.qiniu.model3d.service.CacheService;
import com.qiniu.model3d.service.CacheEvictionService;
import com.qiniu.model3d.service.CacheMetricsService;

import com.qiniu.model3d.service.SimilarityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 缓存服务实现类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);

    @Autowired
    private ModelTaskRepository taskRepository;

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private com.qiniu.model3d.service.CacheWarmupService cacheWarmupService;

    @Autowired
    private CacheMetricsService cacheMetricsService;



    @Value("${cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${cache.exact-match.enabled:true}")
    private boolean exactMatchEnabled;

    @Value("${cache.similarity.threshold:0.85}")
    private double similarityThreshold;

    @Value("${cache.similarity.candidates:50}")
    private int similarityCandidates;

    @Override
    public Optional<ModelTask> findExactMatch(String inputText, ModelTask.TaskType taskType, 
                                            String complexity, String outputFormat) {
        if (!cacheEnabled || !exactMatchEnabled) {
            return Optional.empty();
        }

        try {
            String inputHash = calculateInputHash(inputText, taskType, complexity, outputFormat);
            List<ModelTask> matches = taskRepository.findByInputHashAndCompleted(inputHash);
            
            if (!matches.isEmpty()) {
                ModelTask bestMatch = matches.get(0); // 已按lastAccessed DESC排序
                
                // 验证文件是否存在
                if (isCacheValid(bestMatch)) {
                    updateCacheAccess(bestMatch.getTaskId());
                    logger.info("Found exact cache match for input hash: {}, taskId: {}", 
                              inputHash, bestMatch.getTaskId());
                    return Optional.of(bestMatch);
                } else {
                    logger.warn("Cache file not found for task: {}, removing from cache", 
                              bestMatch.getTaskId());
                    // 文件不存在，标记为失效
                    invalidateCache(bestMatch);
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding exact match cache", e);
            return Optional.empty();
        }
    }

    @Override
    public List<CacheResult> findSimilarMatches(String inputText, ModelTask.TaskType taskType,
                                              String complexity, String outputFormat, double threshold) {
        if (!cacheEnabled) {
            return List.of();
        }

        try {
            // 首先尝试完全匹配
            Optional<ModelTask> exactMatch = findExactMatch(inputText, taskType, complexity, outputFormat);
            if (exactMatch.isPresent()) {
                return List.of(new CacheResult(exactMatch.get(), 1.0, "EXACT"));
            }

            // 获取相似度匹配候选
            List<ModelTask> candidates = taskRepository.findSimilarityCandidates(
                taskType, complexity, outputFormat, PageRequest.of(0, similarityCandidates));

            if (candidates.isEmpty()) {
                return List.of();
            }

            // 计算相似度并分类
            return candidates.stream()
                .map(task -> {
                    double similarity = similarityService.calculateSimilarity(inputText, task.getInputText());
                    String matchType = determineMatchType(similarity);
                    return new CacheResult(task, similarity, matchType);
                })
                .filter(result -> result.getSimilarity() >= threshold)
                .filter(result -> isCacheValid(result.getTask()))
                .peek(result -> {
                    // 更新访问统计
                    updateCacheAccess(result.getTask().getTaskId());
                    
                    // 如果不是精确匹配，更新相似度使用计数
                    if (!"EXACT".equals(result.getMatchType())) {
                        updateSimilarityUsage(result.getTask().getTaskId());
                    }
                    
                    logger.debug("Found similar match: taskId={}, similarity={}, type={}", 
                               result.getTask().getTaskId(), result.getSimilarity(), result.getMatchType());
                })
                .sorted((r1, r2) -> {
                    // 优先按相似度排序，相似度相同时按最后访问时间排序
                    int similarityCompare = Double.compare(r2.getSimilarity(), r1.getSimilarity());
                    if (similarityCompare != 0) {
                        return similarityCompare;
                    }
                    return r2.getTask().getLastAccessed().compareTo(r1.getTask().getLastAccessed());
                })
                .limit(10) // 最多返回10个结果
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error finding similar matches", e);
            return List.of();
        }
    }

    /**
     * 根据相似度确定匹配类型
     */
    private String determineMatchType(double similarity) {
        if (similarityService.isExactMatch(similarity)) {
            return "EXACT";
        } else if (similarityService.isHighSimilarity(similarity)) {
            return "SEMANTIC";
        } else if (similarityService.isMediumSimilarity(similarity)) {
            return "BASIC";
        } else if (similarityService.isLowSimilarity(similarity)) {
            return "WEAK";
        } else {
            return "NONE";
        }
    }

    @Override
    @Transactional
    public boolean cacheTask(ModelTask task) {
        if (!cacheEnabled || task == null || task.getStatus() != ModelTask.TaskStatus.COMPLETED) {
            return false;
        }

        try {
            // 检查是否需要清理缓存
            if (cacheEvictionService.shouldPerformCleanup()) {
                cacheEvictionService.performCacheCleanup();
            }

            // 计算输入哈希
            String inputHash = calculateInputHash(task.getInputText(), task.getType(), 
                                                task.getComplexity() != null ? task.getComplexity().toString() : null, 
                                                task.getOutputFormat() != null ? task.getOutputFormat().toString() : null);
            task.setInputHash(inputHash);

            // 计算文件签名
            if (task.getModelFilePath() != null) {
                String fileSignature = calculateFileSignature(task.getModelFilePath());
                task.setFileSignature(fileSignature);
            }

            // 设置缓存相关字段
            if (task.getReferenceCount() == null) {
                task.setReferenceCount(1);
            }
            if (task.getAccessCount() == null) {
                task.setAccessCount(1);
            }
            if (task.getLastAccessed() == null) {
                task.setLastAccessed(LocalDateTime.now());
            }

            // 设置新的缓存字段
            task.setCached(true);
            if (task.getCacheHitCount() == null) {
                task.setCacheHitCount(0);
            }
            if (task.getSimilarityUsageCount() == null) {
                task.setSimilarityUsageCount(0);
            }
            task.setLastAccessedAt(LocalDateTime.now());

            // 设置文件路径
            if (task.getModelFilePath() != null) {
                String filePath = task.getModelFilePath();
                if (filePath.endsWith(".obj")) {
                    task.setObjFilePath(filePath);
                } else if (filePath.endsWith(".gltf") || filePath.endsWith(".glb")) {
                    task.setGltfFilePath(filePath);
                } else if (filePath.endsWith(".stl")) {
                    task.setStlFilePath(filePath);
                }
            }

            taskRepository.save(task);
            logger.info("Cached task: {}, inputHash: {}", task.getTaskId(), inputHash);
            return true;

        } catch (Exception e) {
            logger.error("Error caching task: " + task.getTaskId(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateCacheAccess(String taskId) {
        try {
            // 更新传统的访问信息
            taskRepository.updateAccessInfo(taskId, LocalDateTime.now());
            
            // 更新新的缓存命中计数
            taskRepository.incrementCacheHitCount(taskId, LocalDateTime.now());
            
            logger.debug("Updated cache access for task: {}", taskId);
            return true;
        } catch (Exception e) {
            logger.error("Error updating cache access for task: " + taskId, e);
            return false;
        }
    }

    /**
     * 更新相似度使用计数
     */
    @Override
    @Transactional
    public boolean updateSimilarityUsage(String taskId) {
        try {
            taskRepository.incrementSimilarityUsageCount(taskId);
            logger.debug("Updated similarity usage for task: {}", taskId);
            return true;
        } catch (Exception e) {
            logger.error("Error updating similarity usage for task: " + taskId, e);
            return false;
        }
    }

    @Override
    public boolean isCacheValid(ModelTask task) {
        if (task == null || task.getModelFilePath() == null) {
            return false;
        }

        try {
            Path filePath = Paths.get(task.getModelFilePath());
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (Exception e) {
            logger.warn("Error checking cache validity for task: " + task.getTaskId(), e);
            return false;
        }
    }

    @Override
    public CacheService.CacheStatistics getCacheStatistics() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7); // 最近7天的统计
            Object[] stats = taskRepository.getCacheHitStatistics(since);
            
            long hitCount = stats[0] != null ? ((Number) stats[0]).longValue() : 0;
            long totalCount = stats[1] != null ? ((Number) stats[1]).longValue() : 0;
            long missCount = Math.max(0, totalCount - hitCount);
            
            // 计算存储大小（简化实现）
            long storageSize = calculateStorageSize();
            
            return new CacheService.CacheStatistics(totalCount, hitCount, missCount, storageSize);
        } catch (Exception e) {
            logger.error("Error getting cache statistics", e);
            return new CacheService.CacheStatistics(0, 0, 0, 0);
        }
    }

    @Override
    public int performCacheCleanup() {
        // 这里只是基础实现，智能淘汰策略在第三阶段实现
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(30);
            List<ModelTask> expiredTasks = taskRepository.findExpiredCacheTasks(expireTime);
            
            int cleanedCount = 0;
            for (ModelTask task : expiredTasks) {
                if (cleanupTask(task)) {
                    cleanedCount++;
                }
            }
            
            logger.info("Cache cleanup completed, cleaned {} tasks", cleanedCount);
            return cleanedCount;
        } catch (Exception e) {
            logger.error("Error during cache cleanup", e);
            return 0;
        }
    }

    @Override
    public int warmUpCache() {
        // 缓存预热的基础实现
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<ModelTask> recentTasks = taskRepository.findRecentSuccessfulTasks(
                since, PageRequest.of(0, 100));
            
            int warmedCount = 0;
            for (ModelTask task : recentTasks) {
                if (isCacheValid(task)) {
                    // 预加载到内存或进行其他预热操作
                    warmedCount++;
                }
            }
            
            logger.info("Cache warm-up completed, warmed {} tasks", warmedCount);
            return warmedCount;
        } catch (Exception e) {
            logger.error("Error during cache warm-up", e);
            return 0;
        }
    }

    /**
     * 计算输入哈希
     */
    @Override
    public String calculateInputHash(String inputText, ModelTask.TaskType taskType, 
                                    String complexity, String outputFormat) {
        try {
            String combined = inputText + "|" + taskType + "|" + 
                            (complexity != null ? complexity : "") + "|" + 
                            (outputFormat != null ? outputFormat : "");
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error calculating input hash", e);
            return null;
        }
    }

    /**
     * 计算文件签名
     */
    @Override
    public String calculateFileSignature(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return null;
            }
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] hash = md.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error calculating file signature for: " + filePath, e);
            return null;
        }
    }

    /**
     * 使缓存失效
     */
    private void invalidateCache(ModelTask task) {
        try {
            task.setReferenceCount(0);
            taskRepository.save(task);
        } catch (Exception e) {
            logger.error("Error invalidating cache for task: " + task.getTaskId(), e);
        }
    }

    /**
     * 清理单个任务
     */
    private boolean cleanupTask(ModelTask task) {
        try {
            if (task.getModelFilePath() != null) {
                Path filePath = Paths.get(task.getModelFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            }
            taskRepository.delete(task);
            return true;
        } catch (Exception e) {
            logger.error("Error cleaning up task: " + task.getTaskId(), e);
            return false;
        }
    }

    /**
     * 计算存储大小
     */
    private long calculateStorageSize() {
        // 简单实现，实际应该计算所有缓存文件的大小
        return 0L;
    }

    /**
     * 强制清理缓存
     */
    @Override
    @Transactional
    public int forceCacheCleanup() {
        try {
            cacheEvictionService.performCacheCleanup();
            // 由于performCacheCleanup()返回void，我们使用forceEvictCache来获取清理数量
            return cacheEvictionService.forceEvictCache(100); // 强制清理100个缓存项
        } catch (Exception e) {
            logger.error("Error during force cache cleanup", e);
            return 0;
        }
    }

    /**
     * 获取缓存统计信息
     */
    @Override
    public CacheEvictionService.CacheStatistics getCacheEvictionStatistics() {
        try {
            return cacheEvictionService.getCacheStatistics();
        } catch (Exception e) {
            logger.error("Error getting cache eviction statistics", e);
            return new CacheEvictionService.CacheStatistics(0, 0, 0, 0.0, 0L);
        }
    }

    /**
     * 检查是否需要缓存清理
     */
    @Override
    public boolean shouldPerformCleanup() {
        try {
            return cacheEvictionService.shouldPerformCleanup();
        } catch (Exception e) {
            logger.error("Error checking if cleanup is needed", e);
            return false;
        }
    }

    /**
     * 获取缓存淘汰候选
     */
    @Override
    public List<ModelTask> getEvictionCandidates(int limit) {
        try {
            return cacheEvictionService.getCandidatesForEviction(limit);
        } catch (Exception e) {
            logger.error("Error getting eviction candidates", e);
            return List.of();
        }
    }

    /**
     * 强制淘汰指定任务
     */
    @Override
    @Transactional
    public boolean forceEvictTask(String taskId) {
        try {
            return cacheEvictionService.forceEvict(taskId);
        } catch (Exception e) {
            logger.error("Error force evicting task: {}", taskId, e);
            return false;
        }
    }

    /**
     * 执行智能缓存预热
     */
    @Override
    public int performIntelligentWarmup() {
        try {
            return cacheWarmupService.performWarmup();
        } catch (Exception e) {
            logger.error("Error performing intelligent warmup", e);
            return 0;
        }
    }

    /**
     * 获取预热统计信息
     */
    @Override
    public com.qiniu.model3d.service.CacheWarmupService.WarmupStatistics getWarmupStatistics() {
        try {
            return cacheWarmupService.getWarmupStatistics();
        } catch (Exception e) {
            logger.error("Error getting warmup statistics", e);
            return new com.qiniu.model3d.service.CacheWarmupService.WarmupStatistics(
                0, 0, 0, 0, 0, null, 0);
        }
    }
}