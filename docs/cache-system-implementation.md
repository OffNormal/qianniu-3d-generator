# 智能缓存系统功能实现文档

## 1. 概述

### 1.1 目标
通过实现智能缓存系统，减少对第三方3D模型生成API的调用次数，预期减少40-70%的API调用，同时将缓存命中时的响应时间从30秒降至2-3秒。

### 1.2 核心策略
- **完全匹配缓存**：相同输入参数直接返回已生成模型
- **相似度匹配缓存**：高相似度输入返回相近模型
- **智能文件复用**：避免重复存储相同模型文件

## 2. 数据库设计

### 2.1 ModelTask表扩展字段

在现有 `ModelTask` 实体类基础上，需要添加以下缓存相关字段：

```sql
-- 添加缓存相关字段
ALTER TABLE model_tasks ADD COLUMN input_hash VARCHAR(64) COMMENT '输入参数哈希值';
ALTER TABLE model_tasks ADD COLUMN cache_hit BOOLEAN DEFAULT FALSE COMMENT '是否为缓存命中';
ALTER TABLE model_tasks ADD COLUMN source_task_id VARCHAR(50) COMMENT '缓存源任务ID';
ALTER TABLE model_tasks ADD COLUMN similarity_score DECIMAL(5,4) COMMENT '相似度分数(0-1)';
ALTER TABLE model_tasks ADD COLUMN cache_created_at TIMESTAMP COMMENT '缓存创建时间';

-- 新增字段：文件管理和访问统计
ALTER TABLE model_tasks ADD COLUMN reference_count INT DEFAULT 1 COMMENT '文件引用计数';
ALTER TABLE model_tasks ADD COLUMN last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间';
ALTER TABLE model_tasks ADD COLUMN access_count INT DEFAULT 1 COMMENT '访问次数';
ALTER TABLE model_tasks ADD COLUMN file_signature VARCHAR(64) COMMENT '文件内容哈希';

-- 优化索引设计
CREATE INDEX idx_input_hash ON model_tasks(input_hash);
CREATE INDEX idx_cache_lookup ON model_tasks(input_hash, status, complexity, output_format);

-- 复合索引优化相似度查询
CREATE INDEX idx_similarity_lookup ON model_tasks(
    type, status, complexity, output_format, created_at DESC
);

-- 缓存管理索引
CREATE INDEX idx_cache_management ON model_tasks(cache_hit, last_accessed, reference_count);
CREATE INDEX idx_file_signature ON model_tasks(file_signature);
```

### 2.2 缓存专用表（可选 - 性能隔离）

```sql
-- 缓存元数据表，用于更好的性能隔离
CREATE TABLE model_cache_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    file_signature VARCHAR(64) COMMENT '文件内容哈希',
    reference_count INT DEFAULT 1,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    access_count INT DEFAULT 1,
    eviction_score DECIMAL(5,4) COMMENT '淘汰评分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_input_hash (input_hash),
    KEY idx_last_accessed (last_accessed),
    KEY idx_eviction_score (eviction_score DESC)
);
```

### 2.3 字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| input_hash | VARCHAR(64) | 输入文本+复杂度+格式的SHA256哈希值 |
| cache_hit | BOOLEAN | 标记该任务是否为缓存命中生成 |
| source_task_id | VARCHAR(50) | 如果是缓存命中，记录原始任务ID |
| similarity_score | DECIMAL(5,4) | 相似度匹配时的分数(0-1) |
| cache_created_at | TIMESTAMP | 缓存任务创建时间 |
| reference_count | INT | 文件引用计数，用于安全删除 |
| last_accessed | TIMESTAMP | 最后访问时间，用于LRU淘汰 |
| access_count | INT | 访问次数，用于LFU淘汰 |
| file_signature | VARCHAR(64) | 文件内容哈希，用于去重 |

## 3. 核心算法设计

### 3.1 哈希计算算法

```java
/**
 * 计算输入参数的哈希值
 * 组合：文本内容 + 复杂度 + 输出格式
 */
public String calculateInputHash(String inputText, Complexity complexity, OutputFormat format) {
    String combined = inputText.trim().toLowerCase() + "|" + 
                     complexity.toString() + "|" + 
                     format.toString();
    return DigestUtils.sha256Hex(combined);
}
```

### 3.2 增强的文本相似度算法

采用多层次相似度计算，结合语义理解：

1. **基础文本相似度**：词汇重叠、编辑距离、关键词匹配
2. **语义相似度**：使用文本嵌入模型计算语义相似度
3. **长度差异惩罚**：对长度差异过大的文本进行惩罚

```java
@Service
public class EnhancedTextSimilarityCalculator {
    
    @Autowired
    private TextEmbeddingService textEmbeddingService;
    
    /**
     * 改进的相似度算法 - 考虑语义相似度
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数 (0-1)
     */
    public double calculateTextSimilarity(String text1, String text2) {
        // 1. 完全匹配
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // 2. 基础文本相似度
        double basicSimilarity = calculateBasicSimilarity(text1, text2);
        
        // 3. 语义相似度（新增）
        double semanticSimilarity = calculateSemanticSimilarity(text1, text2);
        
        // 4. 长度差异惩罚
        double lengthPenalty = calculateLengthPenalty(text1, text2);
        
        // 5. 加权组合
        return Math.max(0, basicSimilarity * 0.6 + semanticSimilarity * 0.4 - lengthPenalty);
    }
    
    /**
     * 基础文本相似度计算
     */
    private double calculateBasicSimilarity(String text1, String text2) {
        // 1. 词汇相似度 (权重: 0.4)
        double jaccardScore = calculateJaccardSimilarity(text1, text2);
        
        // 2. 编辑距离相似度 (权重: 0.3)
        double levenshteinScore = calculateLevenshteinSimilarity(text1, text2);
        
        // 3. 关键词匹配度 (权重: 0.3)
        double keywordScore = calculateKeywordSimilarity(text1, text2);
        
        return jaccardScore * 0.4 + levenshteinScore * 0.3 + keywordScore * 0.3;
    }
    
    /**
     * 使用预训练模型计算语义相似度
     */
    private double calculateSemanticSimilarity(String text1, String text2) {
        try {
            // 可以使用轻量级句子嵌入模型如Sentence-BERT
            // 或者使用词向量平均等简单方法
            double[] embedding1 = textEmbeddingService.getEmbedding(text1);
            double[] embedding2 = textEmbeddingService.getEmbedding(text2);
            return cosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            // 降级到基础相似度
            log.warn("语义相似度计算失败，降级到基础算法: {}", e.getMessage());
            return calculateBasicSimilarity(text1, text2) * 0.8; // 降级惩罚
        }
    }
    
    /**
     * 长度差异惩罚机制
     */
    private double calculateLengthPenalty(String text1, String text2) {
        int len1 = text1.length();
        int len2 = text2.length();
        
        if (len1 == 0 || len2 == 0) {
            return 0.5; // 空文本惩罚
        }
        
        double lengthRatio = (double) Math.min(len1, len2) / Math.max(len1, len2);
        
        // 长度差异超过50%时开始惩罚
        if (lengthRatio < 0.5) {
            return (1 - lengthRatio) * 0.3;
        }
        
        return 0;
    }
    
    /**
     * 向量余弦相似度
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### 3.3 缓存查找策略

```java
/**
 * 缓存查找流程
 */
public CacheResult findCacheMatch(String inputText, Complexity complexity, OutputFormat format) {
    String inputHash = calculateInputHash(inputText, complexity, format);
    
    // 1. 完全匹配查找
    Optional<ModelTask> exactMatch = findExactMatch(inputHash, complexity, format);
    if (exactMatch.isPresent()) {
        return new CacheResult(exactMatch.get(), 1.0, CacheType.EXACT);
    }
    
    // 2. 相似度匹配查找
    List<ModelTask> candidates = findSimilarTasks(complexity, format);
    for (ModelTask candidate : candidates) {
        double similarity = calculateTextSimilarity(inputText, candidate.getInputText());
        if (similarity >= SIMILARITY_THRESHOLD) { // 阈值: 0.85
            return new CacheResult(candidate, similarity, CacheType.SIMILAR);
        }
    }
    
    return CacheResult.noMatch();
}
```

## 4. 服务层实现

### 4.1 缓存服务接口

```java
public interface CacheService {
    /**
     * 查找缓存匹配
     */
    CacheResult findCacheMatch(String inputText, Complexity complexity, OutputFormat format);
    
    /**
     * 创建缓存任务
     */
    ModelTask createCacheTask(ModelTask sourceTask, String newTaskId, String clientIp, double similarity);
    
    /**
     * 复制模型文件
     */
    void copyModelFiles(ModelTask sourceTask, ModelTask targetTask);
    
    /**
     * 清理过期缓存
     */
    void cleanExpiredCache();
}
```

### 4.2 ModelGenerationService修改

在现有的 `generateFromText` 方法中集成缓存逻辑：

```java
public ModelTask generateFromText(TextGenerationRequest request, String clientIp) {
    // 1. 验证输入
    validateTextRequest(request);
    
    // 2. 缓存查找
    CacheResult cacheResult = cacheService.findCacheMatch(
        request.getText(), 
        request.getComplexity(), 
        request.getFormat()
    );
    
    if (cacheResult.isHit()) {
        // 缓存命中 - 创建缓存任务
        String newTaskId = generateTaskId();
        ModelTask cacheTask = cacheService.createCacheTask(
            cacheResult.getSourceTask(), 
            newTaskId, 
            clientIp, 
            cacheResult.getSimilarity()
        );
        
        // 异步复制文件
        CompletableFuture.runAsync(() -> {
            cacheService.copyModelFiles(cacheResult.getSourceTask(), cacheTask);
            updateTaskToCompleted(cacheTask);
        });
        
        logger.info("缓存命中: taskId={}, sourceTaskId={}, similarity={}", 
                   newTaskId, cacheResult.getSourceTask().getTaskId(), cacheResult.getSimilarity());
        
        return cacheTask;
    }
    
    // 3. 缓存未命中 - 正常流程
    ModelTask task = createNewTask(request, clientIp);
    processTextGenerationAsync(task);
    
    return task;
}
```

## 5. 文件管理策略

### 5.1 文件复用机制

```java
/**
 * 智能文件复用
 * 1. 硬链接：相同文件创建硬链接，节省存储空间
 * 2. 软链接：跨分区时使用软链接
 * 3. 复制：链接失败时复制文件
 */
public void copyModelFiles(ModelTask sourceTask, ModelTask targetTask) {
    try {
        Path sourcePath = Paths.get(sourceTask.getModelFilePath());
        Path targetPath = generateNewModelPath(targetTask);
        
        // 尝试创建硬链接
        try {
            Files.createLink(targetPath, sourcePath);
            logger.info("创建硬链接成功: {} -> {}", sourcePath, targetPath);
        } catch (IOException e) {
            // 硬链接失败，复制文件
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件复制成功: {} -> {}", sourcePath, targetPath);
        }
        
        // 更新任务文件路径
        targetTask.setModelFilePath(targetPath.toString());
        
        // 复制预览图
        if (sourceTask.getPreviewImagePath() != null) {
            copyPreviewImage(sourceTask, targetTask);
        }
        
    } catch (Exception e) {
        logger.error("文件复用失败: sourceTask={}, targetTask={}", 
                    sourceTask.getTaskId(), targetTask.getTaskId(), e);
        throw new RuntimeException("文件复用失败", e);
    }
}
```

### 5.2 智能缓存淘汰策略

```java
/**
 * 智能缓存淘汰策略
 */
@Service
public class CacheEvictionPolicy {
    
    @Value("${cache.max-size:100GB}")
    private String maxCacheSize;
    
    @Value("${cache.max-items:5000}")
    private int maxCacheItems;
    
    @Value("${cache.cleanup-threshold:0.9}")
    private double cleanupThreshold;
    
    /**
     * 基于多种因素的加权评分
     */
    public double calculateEvictionScore(ModelTask task) {
        double score = 0;
        
        // 1. 访问频率（权重：0.4）
        double accessFrequency = Math.min(task.getAccessCount(), 100) / 100.0;
        score += (1 - accessFrequency) * 0.4;
        
        // 2. 最近访问时间（权重：0.3）
        double recencyScore = getRecencyScore(task.getLastAccessed());
        score += recencyScore * 0.3;
        
        // 3. 文件大小（权重：0.2）- 优先淘汰大文件
        double sizeScore = Math.min(task.getFileSize(), 100 * 1024 * 1024) / (100.0 * 1024 * 1024);
        score += sizeScore * 0.2;
        
        // 4. 生成成本（权重：0.1）- 高成本任务保留更久
        double costScore = getGenerationCostScore(task);
        score += (1 - costScore) * 0.1;
        
        return score;
    }
    
    /**
     * 计算时间新近度评分
     */
    private double getRecencyScore(LocalDateTime lastAccessed) {
        if (lastAccessed == null) {
            return 1.0; // 从未访问，优先淘汰
        }
        
        long daysSinceAccess = ChronoUnit.DAYS.between(lastAccessed, LocalDateTime.now());
        
        if (daysSinceAccess <= 1) return 0.0;      // 1天内访问
        if (daysSinceAccess <= 7) return 0.2;      // 1周内访问
        if (daysSinceAccess <= 30) return 0.5;     // 1月内访问
        if (daysSinceAccess <= 90) return 0.8;     // 3月内访问
        
        return 1.0; // 超过3月未访问
    }
    
    /**
     * 计算生成成本评分
     */
    private double getGenerationCostScore(ModelTask task) {
        // 基于复杂度和生成时间计算成本
        double complexityScore = task.getComplexity().ordinal() / 3.0; // 假设有4个复杂度级别
        
        if (task.getCompletedAt() != null && task.getCreatedAt() != null) {
            long generationMinutes = ChronoUnit.MINUTES.between(task.getCreatedAt(), task.getCompletedAt());
            double timeScore = Math.min(generationMinutes, 60) / 60.0; // 最大1小时
            return (complexityScore + timeScore) / 2.0;
        }
        
        return complexityScore;
    }
    
    /**
     * 执行智能淘汰
     */
    public void performSmartEviction() {
        // 1. 检查是否需要清理
        if (!needsEviction()) {
            return;
        }
        
        // 2. 获取所有缓存任务
        List<ModelTask> cacheTasks = modelTaskRepository.findAllCacheTasks();
        
        // 3. 计算淘汰评分
        List<TaskEvictionScore> scores = cacheTasks.stream()
            .map(task -> new TaskEvictionScore(task, calculateEvictionScore(task)))
            .sorted(Comparator.comparing(TaskEvictionScore::getScore).reversed())
            .collect(Collectors.toList());
        
        // 4. 淘汰评分最高的任务
        int itemsToEvict = calculateItemsToEvict(cacheTasks.size());
        
        for (int i = 0; i < itemsToEvict && i < scores.size(); i++) {
            ModelTask taskToEvict = scores.get(i).getTask();
            
            try {
                evictTask(taskToEvict);
                logger.info("智能淘汰缓存: taskId={}, score={}", 
                           taskToEvict.getTaskId(), scores.get(i).getScore());
            } catch (Exception e) {
                logger.error("淘汰缓存失败: taskId={}", taskToEvict.getTaskId(), e);
            }
        }
    }
    
    /**
     * 检查是否需要淘汰
     */
    private boolean needsEviction() {
        long currentCacheSize = getCurrentCacheSize();
        long maxSize = parseSize(maxCacheSize);
        
        int currentItems = modelTaskRepository.countCacheTasks();
        
        return (currentCacheSize > maxSize * cleanupThreshold) || 
               (currentItems > maxCacheItems * cleanupThreshold);
    }
    
    /**
     * 计算需要淘汰的项目数
     */
    private int calculateItemsToEvict(int currentItems) {
        if (currentItems > maxCacheItems) {
            return currentItems - (int)(maxCacheItems * 0.8); // 清理到80%
        }
        
        return Math.max(1, currentItems / 10); // 至少清理10%
    }
    
    /**
     * 淘汰单个任务
     */
    private void evictTask(ModelTask task) {
        // 检查引用计数
        if (task.getReferenceCount() > 1) {
            // 减少引用计数而不删除文件
            task.setReferenceCount(task.getReferenceCount() - 1);
            modelTaskRepository.save(task);
        } else {
            // 删除文件和记录
            deleteTaskFiles(task);
            modelTaskRepository.delete(task);
        }
    }
    
    /**
     * 任务淘汰评分包装类
     */
    @Data
    @AllArgsConstructor
    private static class TaskEvictionScore {
        private ModelTask task;
        private double score;
    }
}

/**
 * 定时清理过期缓存
 */
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void cleanExpiredCache() {
    LocalDateTime expireTime = LocalDateTime.now().minusDays(CACHE_EXPIRE_DAYS); // 30天
    
    List<ModelTask> expiredTasks = modelTaskRepository.findExpiredCacheTasks(expireTime);
    
    for (ModelTask task : expiredTasks) {
        try {
            // 删除文件
            deleteTaskFiles(task);
            
            // 删除数据库记录
            modelTaskRepository.delete(task);
            
            logger.info("清理过期缓存: taskId={}", task.getTaskId());
        } catch (Exception e) {
            logger.error("清理缓存失败: taskId={}", task.getTaskId(), e);
        }
    }
    
    // 执行智能淘汰
    cacheEvictionPolicy.performSmartEviction();
}
```

## 6. 并发控制和缓存预热

### 6.1 缓存并发控制

```java
/**
 * 缓存并发控制服务
 * 防止缓存击穿，确保相同请求只执行一次
 */
@Service
public class CacheConcurrencyService {
    
    private final ConcurrentHashMap<String, CompletableFuture<ModelTask>> ongoingTasks = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(CacheConcurrencyService.class);
    
    /**
     * 防止缓存击穿 - 相同请求只执行一次
     */
    public ModelTask getOrCreateTask(String cacheKey, Supplier<ModelTask> taskSupplier) {
        try {
            CompletableFuture<ModelTask> future = ongoingTasks.computeIfAbsent(cacheKey, key -> {
                logger.info("开始执行任务: cacheKey={}", key);
                return CompletableFuture.supplyAsync(taskSupplier)
                    .whenComplete((result, error) -> {
                        ongoingTasks.remove(key);
                        if (error != null) {
                            logger.error("任务执行失败: cacheKey={}", key, error);
                        } else {
                            logger.info("任务执行完成: cacheKey={}, taskId={}", key, result.getTaskId());
                        }
                    });
            });
            
            return future.get(300, TimeUnit.SECONDS); // 5分钟超时
            
        } catch (TimeoutException e) {
            logger.error("任务执行超时: cacheKey={}", cacheKey);
            ongoingTasks.remove(cacheKey);
            throw new RuntimeException("任务执行超时", e);
        } catch (Exception e) {
            logger.error("任务执行异常: cacheKey={}", cacheKey, e);
            ongoingTasks.remove(cacheKey);
            throw new RuntimeException("任务执行失败", e);
        }
    }
    
    /**
     * 获取当前正在执行的任务数量
     */
    public int getOngoingTaskCount() {
        return ongoingTasks.size();
    }
    
    /**
     * 清理超时的任务
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void cleanupTimeoutTasks() {
        ongoingTasks.entrySet().removeIf(entry -> {
            CompletableFuture<ModelTask> future = entry.getValue();
            if (future.isDone() || future.isCancelled()) {
                logger.debug("清理已完成的任务: cacheKey={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
```

### 6.2 缓存预热服务

```java
/**
 * 缓存预热服务
 * 在应用启动时预热高频查询的缓存
 */
@Service
public class CacheWarmUpService {
    
    private final ModelTaskRepository modelTaskRepository;
    private final CacheService cacheService;
    private final Logger logger = LoggerFactory.getLogger(CacheWarmUpService.class);
    
    @Value("${cache.warmup.enabled:true}")
    private boolean warmupEnabled;
    
    @Value("${cache.warmup.popular-tasks-limit:100}")
    private int popularTasksLimit;
    
    public CacheWarmUpService(ModelTaskRepository modelTaskRepository, CacheService cacheService) {
        this.modelTaskRepository = modelTaskRepository;
        this.cacheService = cacheService;
    }
    
    /**
     * 应用启动时预热缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCache() {
        if (!warmupEnabled) {
            logger.info("缓存预热已禁用");
            return;
        }
        
        logger.info("开始缓存预热...");
        
        try {
            // 1. 预热高频查询的缓存
            warmUpPopularTasks();
            
            // 2. 预热最近的成功任务
            warmUpRecentSuccessfulTasks();
            
            logger.info("缓存预热完成");
            
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }
    
    /**
     * 预热热门任务
     */
    private void warmUpPopularTasks() {
        logger.info("预热热门任务缓存...");
        
        // 查询访问次数最多的任务
        List<ModelTask> popularTasks = modelTaskRepository.findPopularTasks(popularTasksLimit);
        
        for (ModelTask task : popularTasks) {
            try {
                preloadToCache(task);
                Thread.sleep(100); // 避免过快的预热影响系统性能
            } catch (Exception e) {
                logger.warn("预热任务失败: taskId={}", task.getTaskId(), e);
            }
        }
        
        logger.info("热门任务预热完成，共预热 {} 个任务", popularTasks.size());
    }
    
    /**
     * 预热最近的成功任务
     */
    private void warmUpRecentSuccessfulTasks() {
        logger.info("预热最近成功任务缓存...");
        
        LocalDateTime since = LocalDateTime.now().minusDays(7); // 最近7天
        List<ModelTask> recentTasks = modelTaskRepository.findRecentSuccessfulTasks(since, 50);
        
        for (ModelTask task : recentTasks) {
            try {
                preloadToCache(task);
                Thread.sleep(50);
            } catch (Exception e) {
                logger.warn("预热最近任务失败: taskId={}", task.getTaskId(), e);
            }
        }
        
        logger.info("最近任务预热完成，共预热 {} 个任务", recentTasks.size());
    }
    
    /**
     * 将任务预加载到缓存
     */
    private void preloadToCache(ModelTask task) {
        if (task.getStatus() == TaskStatus.COMPLETED && task.getFilePath() != null) {
            // 更新访问时间和访问次数
            task.setLastAccessed(LocalDateTime.now());
            task.setAccessCount(task.getAccessCount() + 1);
            modelTaskRepository.save(task);
            
            logger.debug("预热缓存: taskId={}, inputText={}", 
                        task.getTaskId(), 
                        task.getInputText().substring(0, Math.min(50, task.getInputText().length())));
        }
    }
    
    /**
     * 手动触发缓存预热
     */
    @Async
    public void manualWarmUp() {
        logger.info("手动触发缓存预热");
        warmUpCache();
    }
}
```

### 6.3 Repository扩展

```java
/**
 * ModelTaskRepository 扩展方法
 */
public interface ModelTaskRepository extends JpaRepository<ModelTask, Long> {
    
    // 现有方法...
    
    /**
     * 查询热门任务（按访问次数排序）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'COMPLETED' AND t.accessCount > 0 " +
           "ORDER BY t.accessCount DESC, t.lastAccessed DESC")
    List<ModelTask> findPopularTasks(Pageable pageable);
    
    default List<ModelTask> findPopularTasks(int limit) {
        return findPopularTasks(PageRequest.of(0, limit));
    }
    
    /**
     * 查询最近的成功任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'COMPLETED' AND t.createdAt >= :since " +
           "ORDER BY t.createdAt DESC")
    List<ModelTask> findRecentSuccessfulTasks(@Param("since") LocalDateTime since, Pageable pageable);
    
    default List<ModelTask> findRecentSuccessfulTasks(LocalDateTime since, int limit) {
        return findRecentSuccessfulTasks(since, PageRequest.of(0, limit));
    }
    
    /**
     * 统计正在进行的任务数量
     */
    @Query("SELECT COUNT(t) FROM ModelTask t WHERE t.status IN ('PENDING', 'PROCESSING')")
    long countOngoingTasks();
}
```

## 7. 配置参数

### 7.1 application.yml配置

```yaml
cache:
  # 分层配置
  exact-match:
    enabled: true
    ttl: 30d                    # 完全匹配缓存存活时间
  
  similarity-match:
    enabled: true
    threshold: 0.85             # 相似度阈值
    ttl: 15d                    # 相似匹配缓存存活时间
    max-candidates: 50          # 最大候选数量
    semantic-weight: 0.4        # 语义相似度权重
    basic-weight: 0.6           # 基础相似度权重
  
  # 淘汰策略
  eviction:
    policy: "SMART"             # LRU, LFU, SIZE_BASED, SMART
    max-size: 100GB             # 最大缓存大小
    max-items: 5000             # 最大缓存项目数
    cleanup-threshold: 0.9      # 达到90%容量时开始清理
    
    # 智能淘汰权重配置
    weights:
      access-frequency: 0.4     # 访问频率权重
      recency: 0.3             # 最近访问时间权重
      file-size: 0.2           # 文件大小权重
      generation-cost: 0.1     # 生成成本权重
  
  # 性能优化
  concurrency:
    max-parallel-similarity: 10 # 最大并行相似度计算数
    batch-size: 100            # 批处理大小
    task-timeout: 300          # 任务超时时间(秒)
  
  # 文件管理
  file-management:
    use-hard-link: true        # 是否使用硬链接
    cleanup-schedule: "0 0 2 * * ?"  # 清理计划
    async-file-copy: true      # 异步文件复制
    backup-enabled: false      # 是否启用文件备份
  
  # 缓存预热
  warmup:
    enabled: true              # 是否启用预热
    popular-tasks-limit: 100   # 预热热门任务数量
    recent-days: 7             # 预热最近几天的任务
    startup-delay: 30          # 启动延迟(秒)
  
  # 监控配置
  monitoring:
    metrics-enabled: true      # 是否启用指标收集
    statistics-interval: 300   # 统计间隔(秒)
    alert-threshold: 0.1       # 告警阈值(命中率低于10%)
```

### 6.2 常量定义

```java
public class CacheConstants {
    public static final double SIMILARITY_THRESHOLD = 0.85;
    public static final int CACHE_EXPIRE_DAYS = 30;
    public static final int MAX_CACHE_SIZE = 1000;
    public static final int SIMILARITY_SEARCH_LIMIT = 100;
    
    public enum CacheType {
        EXACT,    // 完全匹配
        SIMILAR,  // 相似匹配
        NONE      // 无匹配
    }
}
```

## 8. 监控和统计

### 8.1 增强的监控指标

```java
/**
 * 增强的缓存监控指标
 */
@Component
public class CacheMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 缓存命中率细分
    @Gauge(name = "cache_hit_rate_exact", description = "完全匹配缓存命中率")
    private final AtomicDouble exactHitRate = new AtomicDouble(0.0);
    
    @Gauge(name = "cache_hit_rate_similar", description = "相似匹配缓存命中率")
    private final AtomicDouble similarHitRate = new AtomicDouble(0.0);
    
    @Gauge(name = "cache_hit_rate_total", description = "总体缓存命中率")
    private final AtomicDouble totalHitRate = new AtomicDouble(0.0);
    
    // 性能指标
    @Timer(name = "cache_lookup_duration", description = "缓存查找耗时")
    private final Timer lookupTimer;
    
    @Timer(name = "similarity_calculation_duration", description = "相似度计算耗时")
    private final Timer similarityTimer;
    
    @Timer(name = "file_copy_duration", description = "文件复制耗时")
    private final Timer fileCopyTimer;
    
    // 业务指标
    @Counter(name = "api_calls_saved_daily", description = "每日节省的API调用数")
    private final Counter dailyApiSaves;
    
    @Gauge(name = "storage_space_saved", description = "节省的存储空间(GB)")
    private final AtomicDouble storageSaved = new AtomicDouble(0.0);
    
    @Gauge(name = "cache_size_current", description = "当前缓存大小(GB)")
    private final AtomicDouble currentCacheSize = new AtomicDouble(0.0);
    
    @Gauge(name = "cache_items_count", description = "缓存项目数量")
    private final AtomicLong cacheItemsCount = new AtomicLong(0);
    
    // 错误指标
    @Counter(name = "cache_errors_total", description = "缓存错误总数")
    private final Counter cacheErrors;
    
    @Counter(name = "file_copy_errors", description = "文件复制错误数")
    private final Counter fileCopyErrors;
    
    public CacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.lookupTimer = Timer.builder("cache_lookup_duration").register(meterRegistry);
        this.similarityTimer = Timer.builder("similarity_calculation_duration").register(meterRegistry);
        this.fileCopyTimer = Timer.builder("file_copy_duration").register(meterRegistry);
        this.dailyApiSaves = Counter.builder("api_calls_saved_daily").register(meterRegistry);
        this.cacheErrors = Counter.builder("cache_errors_total").register(meterRegistry);
        this.fileCopyErrors = Counter.builder("file_copy_errors").register(meterRegistry);
    }
    
    /**
     * 记录缓存命中
     */
    public void recordCacheHit(CacheType cacheType) {
        switch (cacheType) {
            case EXACT:
                exactHitRate.addAndGet(1);
                break;
            case SIMILAR:
                similarHitRate.addAndGet(1);
                break;
        }
        dailyApiSaves.increment();
    }
    
    /**
     * 记录缓存错误
     */
    public void recordCacheError(String errorType) {
        cacheErrors.increment(Tags.of("type", errorType));
    }
    
    /**
     * 更新缓存大小统计
     */
    public void updateCacheSize(double sizeInGB, long itemCount) {
        currentCacheSize.set(sizeInGB);
        cacheItemsCount.set(itemCount);
    }
}
```

### 8.2 缓存统计服务

```java
/**
 * 增强的缓存统计服务
 */
@Service
public class CacheStatisticsService {
    
    private final ModelTaskRepository modelTaskRepository;
    private final CacheMetrics cacheMetrics;
    private final Logger logger = LoggerFactory.getLogger(CacheStatisticsService.class);
    
    /**
     * 获取详细的缓存统计信息
     */
    public DetailedCacheStatistics getDetailedStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 总体统计
        long totalRequests = modelTaskRepository.countByCreatedAtBetween(startTime, endTime);
        long exactHits = modelTaskRepository.countByExactCacheHitAndCreatedAtBetween(true, startTime, endTime);
        long similarHits = modelTaskRepository.countBySimilarCacheHitAndCreatedAtBetween(true, startTime, endTime);
        long totalHits = exactHits + similarHits;
        
        // 计算命中率
        double exactHitRate = totalRequests > 0 ? (double) exactHits / totalRequests : 0;
        double similarHitRate = totalRequests > 0 ? (double) similarHits / totalRequests : 0;
        double totalHitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0;
        
        // 性能统计
        double avgLookupTime = calculateAverageLookupTime(startTime, endTime);
        double avgSimilarityTime = calculateAverageSimilarityTime(startTime, endTime);
        
        // 存储统计
        long totalCacheSize = calculateTotalCacheSize();
        long savedStorage = calculateSavedStorage(totalHits);
        
        // 相似度分布
        Map<String, Long> similarityDistribution = getSimilarityDistribution(startTime, endTime);
        
        return DetailedCacheStatistics.builder()
                .totalRequests(totalRequests)
                .exactHits(exactHits)
                .similarHits(similarHits)
                .totalHits(totalHits)
                .exactHitRate(exactHitRate)
                .similarHitRate(similarHitRate)
                .totalHitRate(totalHitRate)
                .avgLookupTime(avgLookupTime)
                .avgSimilarityTime(avgSimilarityTime)
                .totalCacheSize(totalCacheSize)
                .savedStorage(savedStorage)
                .similarityDistribution(similarityDistribution)
                .build();
    }
    
    /**
     * 获取相似度分布统计
     */
    private Map<String, Long> getSimilarityDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        List<ModelTask> similarTasks = modelTaskRepository.findSimilarCacheHitsBetween(startTime, endTime);
        
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("0.85-0.90", 0L);
        distribution.put("0.90-0.95", 0L);
        distribution.put("0.95-1.00", 0L);
        
        for (ModelTask task : similarTasks) {
            double similarity = task.getCacheSimilarity();
            if (similarity >= 0.85 && similarity < 0.90) {
                distribution.merge("0.85-0.90", 1L, Long::sum);
            } else if (similarity >= 0.90 && similarity < 0.95) {
                distribution.merge("0.90-0.95", 1L, Long::sum);
            } else if (similarity >= 0.95) {
                distribution.merge("0.95-1.00", 1L, Long::sum);
            }
        }
        
        return distribution;
    }
    
    /**
     * 定时更新监控指标
     */
    @Scheduled(fixedRate = 300000) // 每5分钟更新一次
    public void updateMetrics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            DetailedCacheStatistics stats = getDetailedStatistics(oneHourAgo, now);
            
            // 更新Micrometer指标
            cacheMetrics.exactHitRate.set(stats.getExactHitRate());
            cacheMetrics.similarHitRate.set(stats.getSimilarHitRate());
            cacheMetrics.totalHitRate.set(stats.getTotalHitRate());
            
            // 更新缓存大小
            long itemCount = modelTaskRepository.countCacheTasks();
            double sizeInGB = stats.getTotalCacheSize() / (1024.0 * 1024.0 * 1024.0);
            cacheMetrics.updateCacheSize(sizeInGB, itemCount);
            
            logger.debug("缓存指标更新完成: 总命中率={}, 缓存项数={}", 
                        stats.getTotalHitRate(), itemCount);
            
        } catch (Exception e) {
            logger.error("更新缓存指标失败", e);
            cacheMetrics.recordCacheError("metrics_update_failed");
        }
    }
}
```

### 8.3 告警配置

```java
/**
 * 缓存告警服务
 */
@Service
public class CacheAlertService {
    
    @Value("${cache.monitoring.alert-threshold:0.1}")
    private double alertThreshold;
    
    private final CacheStatisticsService statisticsService;
    private final Logger logger = LoggerFactory.getLogger(CacheAlertService.class);
    
    /**
     * 检查缓存性能并发送告警
     */
    @Scheduled(fixedRate = 600000) // 每10分钟检查一次
    public void checkCachePerformance() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        DetailedCacheStatistics stats = statisticsService.getDetailedStatistics(oneHourAgo, now);
        
        // 检查命中率告警
        if (stats.getTotalHitRate() < alertThreshold) {
            sendAlert("缓存命中率过低", 
                     String.format("当前命中率: %.2f%%, 阈值: %.2f%%", 
                                  stats.getTotalHitRate() * 100, alertThreshold * 100));
        }
        
        // 检查存储空间告警
        double cacheSize = stats.getTotalCacheSize() / (1024.0 * 1024.0 * 1024.0); // GB
        if (cacheSize > 90) { // 超过90GB
            sendAlert("缓存存储空间不足", 
                     String.format("当前缓存大小: %.2f GB", cacheSize));
        }
        
        // 检查错误率告警
        // 可以根据需要添加更多告警规则
    }
    
    private void sendAlert(String title, String message) {
        logger.warn("缓存告警: {} - {}", title, message);
        // 这里可以集成邮件、短信、钉钉等告警方式
    }
}
```

### 8.4 监控指标说明

#### 缓存命中率指标
- **cache_hit_rate_exact**: 完全匹配缓存命中率
- **cache_hit_rate_similar**: 相似匹配缓存命中率  
- **cache_hit_rate_total**: 总体缓存命中率

#### 性能指标
- **cache_lookup_duration**: 缓存查找耗时
- **similarity_calculation_duration**: 相似度计算耗时
- **file_copy_duration**: 文件复制耗时

#### 业务指标
- **api_calls_saved_daily**: 每日节省的API调用数
- **storage_space_saved**: 节省的存储空间
- **cache_size_current**: 当前缓存大小
- **cache_items_count**: 缓存项目数量

#### 错误指标
- **cache_errors_total**: 缓存错误总数
- **file_copy_errors**: 文件复制错误数

## 8. 详细实施计划

### 8.1 第一阶段：数据库和基础架构（1-2天）

#### 8.1.1 数据库结构优化
```sql
-- 1. 添加新字段
ALTER TABLE model_tasks ADD COLUMN file_size BIGINT COMMENT '文件大小(字节)';
ALTER TABLE model_tasks ADD COLUMN reference_count INT DEFAULT 1 COMMENT '文件引用计数';
ALTER TABLE model_tasks ADD COLUMN last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间';
ALTER TABLE model_tasks ADD COLUMN access_count INT DEFAULT 1 COMMENT '访问次数';
ALTER TABLE model_tasks ADD COLUMN file_signature VARCHAR(64) COMMENT '文件内容哈希';

-- 2. 添加优化索引
CREATE INDEX idx_similarity_lookup ON model_tasks(type, status, complexity, output_format, created_at DESC);
CREATE INDEX idx_last_accessed ON model_tasks(last_accessed);
CREATE INDEX idx_access_count ON model_tasks(access_count);
CREATE INDEX idx_file_signature ON model_tasks(file_signature);

-- 3. 创建缓存专用表（可选）
CREATE TABLE model_cache_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    file_signature VARCHAR(64) COMMENT '文件内容哈希',
    reference_count INT DEFAULT 1,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    access_count INT DEFAULT 1,
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_input_hash (input_hash),
    KEY idx_last_accessed (last_accessed)
);
```

#### 8.1.2 基础服务实现
1. 实现 `CacheService` 接口
2. 实现 `ModelTaskRepository` 扩展方法
3. 配置基础缓存参数
4. 单元测试覆盖

### 8.2 第二阶段：核心缓存功能（2-3天）

#### 8.2.1 完全匹配缓存
1. 实现哈希计算和匹配逻辑
2. 文件复用机制（硬链接/软链接）
3. 缓存任务创建和管理
4. 错误处理和降级策略

#### 8.2.2 相似度匹配系统
1. 实现多层次文本相似度算法
2. 语义相似度计算（可选）
3. 相似度阈值配置和调优
4. 候选缓存排序和选择

#### 8.2.3 集成测试
1. 端到端缓存流程测试
2. 性能基准测试
3. 并发访问测试

### 8.3 第三阶段：智能优化功能（2-3天）

#### 8.3.1 智能淘汰策略
1. 实现 `CacheEvictionPolicy` 服务
2. 多因素评分算法
3. 定时和触发式清理
4. 淘汰效果监控

#### 8.3.2 并发控制和预热
1. 实现 `CacheConcurrencyService`
2. 防缓存击穿机制
3. 实现 `CacheWarmUpService`
4. 应用启动预热逻辑

#### 8.3.3 高级配置
1. 分层缓存配置
2. 性能参数调优
3. 监控告警配置

### 8.4 第四阶段：监控和优化（1-2天）

#### 8.4.1 监控系统
1. 实现 `CacheMetrics` 指标收集
2. 实现 `CacheStatisticsService`
3. 实现 `CacheAlertService`
4. 监控面板配置

#### 8.4.2 性能优化
1. 缓存性能分析
2. 相似度算法优化
3. 存储空间优化
4. 响应时间优化

#### 8.4.3 压力测试
1. 高并发场景测试
2. 大数据量测试
3. 长期运行稳定性测试

### 8.5 第五阶段：上线和维护（1天）

#### 8.5.1 生产部署
1. 灰度发布策略
2. 配置参数调优
3. 监控告警设置
4. 回滚方案准备

#### 8.5.2 运维文档
1. 操作手册编写
2. 故障排查指南
3. 性能调优指南
4. 维护计划制定

## 9. 风险评估与缓解

### 9.1 技术风险

#### 9.1.1 存储空间风险
**风险描述**：缓存文件可能快速增长，占用大量磁盘空间

**缓解措施**：
- 设置最大缓存大小限制（100GB）
- 实施智能淘汰策略
- 定期清理过期缓存
- 监控存储使用率，设置告警

#### 9.1.2 相似度准确性风险
**风险描述**：算法可能产生误匹配，影响用户体验

**缓解措施**：
- 设置保守的相似度阈值（0.85）
- 提供用户反馈机制
- 实施A/B测试验证效果
- 支持手动禁用相似度匹配

#### 9.1.3 文件一致性风险
**风险描述**：硬链接可能导致文件管理复杂，数据不一致

**缓解措施**：
- 实现引用计数机制
- 提供软链接和复制降级方案
- 定期文件完整性检查
- 完善的错误处理和恢复机制

### 9.2 性能风险

#### 9.2.1 相似度计算性能
**风险描述**：大量相似度计算可能影响响应时间

**缓解措施**：
- 限制相似度搜索候选数量（50个）
- 实施并行计算（最多10个线程）
- 设置计算超时机制
- 缓存相似度计算结果

#### 9.2.2 数据库性能
**风险描述**：频繁的缓存查询可能影响数据库性能

**缓解措施**：
- 优化数据库索引设计
- 实施查询结果缓存
- 数据库连接池优化
- 读写分离（如需要）

### 9.3 业务风险

#### 9.3.1 缓存命中率低
**风险描述**：如果缓存命中率过低，投入产出比不佳

**缓解措施**：
- 设置最低命中率告警（30%）
- 持续优化相似度算法
- 分析用户行为模式
- 调整缓存策略

## 10. 预期效果与ROI分析

### 10.1 性能提升指标

#### 10.1.1 响应时间改善
- **完全匹配缓存命中**：从30秒降至2-3秒（90%提升）
- **相似度匹配命中**：从30秒降至5-8秒（75%提升）
- **整体平均响应时间**：预期提升60-80%

#### 10.1.2 系统吞吐量
- **并发处理能力**：提升3-5倍
- **API调用减少**：40-70%
- **服务器资源利用率**：降低50-60%

### 10.2 成本节约分析

#### 10.2.1 直接成本节约
- **第三方API费用**：每月节约60-80%
- **服务器计算资源**：节约50%
- **带宽使用**：减少40%

#### 10.2.2 间接效益
- **用户体验提升**：减少等待时间，提高满意度
- **系统稳定性**：减少第三方依赖，提高可用性
- **开发效率**：减少性能问题排查时间

### 10.3 投资回报率（ROI）

#### 10.3.1 投入成本
- **开发时间**：约8-10人天
- **存储成本**：每月增加约100-200元
- **维护成本**：每月约0.5人天

#### 10.3.2 预期收益
- **API费用节约**：每月5000-10000元
- **服务器成本节约**：每月2000-3000元
- **总体ROI**：预期3-6个月回本

## 11. 总结

### 11.1 核心价值

这个智能缓存系统通过以下核心功能为3D模型生成服务带来显著价值：

1. **多层次缓存策略**：完全匹配和相似度匹配相结合，最大化缓存命中率
2. **智能淘汰机制**：基于多因素评分的智能淘汰，优化存储利用率
3. **高性能架构**：并发控制、缓存预热、异步处理等确保系统高性能
4. **全面监控体系**：细粒度监控指标和告警机制，确保系统稳定运行

### 11.2 技术亮点

- **创新的相似度算法**：结合词汇、语义和关键词匹配的多维度相似度计算
- **智能文件管理**：硬链接优先、多级降级的文件复用策略
- **自适应淘汰策略**：基于访问模式、文件大小、生成成本的综合评分
- **完善的并发控制**：防止缓存击穿，确保系统在高并发下稳定运行

### 11.3 实施建议

1. **分阶段实施**：按照5个阶段逐步推进，确保每个阶段质量
2. **充分测试**：重点关注性能测试和长期稳定性测试
3. **监控先行**：在功能上线前确保监控体系完备
4. **持续优化**：根据实际运行数据持续调优参数和算法

### 11.4 长期规划

- **机器学习优化**：引入ML模型优化相似度计算和淘汰策略
- **分布式缓存**：支持多节点缓存共享和负载均衡
- **用户个性化**：基于用户行为的个性化缓存策略
- **跨服务缓存**：扩展到其他AI服务的缓存复用

通过实施这个智能缓存系统，您的3D模型生成服务将在性能、成本和用户体验方面获得全面提升，为业务发展奠定坚实的技术基础。