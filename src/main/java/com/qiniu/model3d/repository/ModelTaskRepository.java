package com.qiniu.model3d.repository;

import com.qiniu.model3d.entity.ModelTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 模型任务数据访问接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Repository
public interface ModelTaskRepository extends JpaRepository<ModelTask, Long> {

    /**
     * 根据任务ID查找任务
     */
    Optional<ModelTask> findByTaskId(String taskId);

    /**
     * 根据模型ID查找任务
     */
    Optional<ModelTask> findByModelId(String modelId);

    /**
     * 根据状态查找任务
     */
    List<ModelTask> findByStatus(ModelTask.TaskStatus status);

    /**
     * 根据客户端IP查找任务（分页）
     */
    Page<ModelTask> findByClientIpOrderByCreatedAtDesc(String clientIp, Pageable pageable);

    /**
     * 根据客户端IP和状态查找任务（分页）
     */
    Page<ModelTask> findByClientIpAndStatusOrderByCreatedAtDesc(String clientIp, ModelTask.TaskStatus status, Pageable pageable);

    /**
     * 统计指定IP今日生成的任务数量
     */
    @Query("SELECT COUNT(t) FROM ModelTask t WHERE t.clientIp = :clientIp AND t.createdAt >= :startTime")
    long countTodayTasksByClientIp(@Param("clientIp") String clientIp, @Param("startTime") LocalDateTime startTime);

    /**
     * 查找过期的任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status IN ('COMPLETED', 'FAILED') AND t.completedAt < :expireTime")
    List<ModelTask> findExpiredTasks(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 查找超时的处理中任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'PROCESSING' AND t.updatedAt < :timeoutTime")
    List<ModelTask> findTimeoutTasks(@Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 删除指定时间之前的任务
     */
    void deleteByCreatedAtBefore(LocalDateTime time);

    /**
     * 统计各状态的任务数量
     */
    @Query("SELECT t.status, COUNT(t) FROM ModelTask t GROUP BY t.status")
    List<Object[]> countTasksByStatus();

    /**
     * 查找最近完成的任务（用于推荐）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.clientIp = :clientIp AND t.status = 'COMPLETED' ORDER BY t.completedAt DESC")
    List<ModelTask> findRecentCompletedTasks(@Param("clientIp") String clientIp, Pageable pageable);

    // ========== 缓存相关查询方法 ==========

    /**
     * 根据输入哈希查找完全匹配的缓存任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.inputHash = :inputHash AND t.status = 'COMPLETED' ORDER BY t.lastAccessed DESC")
    List<ModelTask> findByInputHashAndCompleted(@Param("inputHash") String inputHash);

    /**
     * 根据文件签名查找任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.fileSignature = :fileSignature AND t.status = 'COMPLETED'")
    List<ModelTask> findByFileSignature(@Param("fileSignature") String fileSignature);

    /**
     * 查找相似度匹配的候选任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.type = :type AND t.status = 'COMPLETED' " +
           "AND (:complexity IS NULL OR t.complexity = :complexity) " +
           "AND (:outputFormat IS NULL OR t.outputFormat = :outputFormat) " +
           "ORDER BY t.lastAccessed DESC")
    List<ModelTask> findSimilarityCandidates(@Param("type") ModelTask.TaskType type,
                                           @Param("complexity") String complexity,
                                           @Param("outputFormat") String outputFormat,
                                           Pageable pageable);

    /**
     * 查找热门任务（按访问次数排序）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'COMPLETED' AND t.accessCount > :minAccessCount " +
           "ORDER BY t.accessCount DESC, t.lastAccessed DESC")
    List<ModelTask> findPopularTasks(@Param("minAccessCount") int minAccessCount, Pageable pageable);

    /**
     * 查找最近成功的任务（用于缓存预热）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'COMPLETED' AND t.completedAt >= :since " +
           "ORDER BY t.completedAt DESC")
    List<ModelTask> findRecentSuccessfulTasks(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 查找需要清理的低引用任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.referenceCount <= :maxRefCount " +
           "AND t.lastAccessed < :lastAccessedBefore " +
           "ORDER BY t.lastAccessed ASC")
    List<ModelTask> findTasksForEviction(@Param("maxRefCount") int maxRefCount,
                                       @Param("lastAccessedBefore") LocalDateTime lastAccessedBefore,
                                       Pageable pageable);

    /**
     * 统计缓存命中率相关数据
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN t.accessCount > 1 THEN 1 END) as hitCount, " +
           "COUNT(t) as totalCount " +
           "FROM ModelTask t WHERE t.status = 'COMPLETED' AND t.createdAt >= :since")
    Object[] getCacheHitStatistics(@Param("since") LocalDateTime since);

    /**
     * 更新任务的访问信息
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.accessCount = t.accessCount + 1, t.lastAccessed = :accessTime " +
           "WHERE t.taskId = :taskId")
    void updateAccessInfo(@Param("taskId") String taskId, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 更新引用计数
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.referenceCount = :refCount WHERE t.taskId = :taskId")
    void updateReferenceCount(@Param("taskId") String taskId, @Param("refCount") int refCount);

    /**
     * 查找过期的缓存任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.status = 'COMPLETED' " +
           "AND t.lastAccessed < :expireTime " +
           "AND t.referenceCount <= 1")
    List<ModelTask> findExpiredCacheTasks(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 查找缓存的任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED'")
    List<ModelTask> findCachedTasks();

    /**
     * 根据最后访问时间查找缓存任务（LRU淘汰）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "ORDER BY t.lastAccessedAt ASC")
    List<ModelTask> findCachedTasksByLRU(Pageable pageable);

    /**
     * 根据访问频率查找缓存任务（LFU淘汰）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "ORDER BY t.cacheHitCount ASC, t.lastAccessedAt ASC")
    List<ModelTask> findCachedTasksByLFU(Pageable pageable);

    /**
     * 查找低价值的缓存任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "AND t.cacheHitCount <= :maxHitCount " +
           "AND t.lastAccessedAt < :lastAccessedBefore " +
           "ORDER BY t.cacheHitCount ASC, t.lastAccessedAt ASC")
    List<ModelTask> findLowValueCacheTasks(@Param("maxHitCount") int maxHitCount,
                                         @Param("lastAccessedBefore") LocalDateTime lastAccessedBefore,
                                         Pageable pageable);

    /**
     * 统计缓存使用情况
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN t.cached = true THEN 1 END) as cachedCount, " +
           "COUNT(t) as totalCount, " +
           "AVG(CASE WHEN t.cached = true THEN t.cacheHitCount ELSE 0 END) as avgHitCount, " +
           "SUM(CASE WHEN t.cached = true THEN t.similarityUsageCount ELSE 0 END) as totalSimilarityUsage " +
           "FROM ModelTask t WHERE t.status = 'COMPLETED'")
    Object[] getCacheUsageStatistics();

    /**
     * 查找需要清理的大文件缓存任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "AND (LENGTH(t.objFilePath) > 0 OR LENGTH(t.gltfFilePath) > 0 OR LENGTH(t.stlFilePath) > 0) " +
           "AND t.cacheHitCount <= :maxHitCount " +
           "ORDER BY t.cacheHitCount ASC, t.lastAccessedAt ASC")
    List<ModelTask> findLargeFileCacheTasks(@Param("maxHitCount") int maxHitCount, Pageable pageable);

    /**
     * 更新缓存状态
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.cached = :cached WHERE t.taskId = :taskId")
    void updateCacheStatus(@Param("taskId") String taskId, @Param("cached") boolean cached);

    /**
     * 更新缓存命中计数
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.cacheHitCount = t.cacheHitCount + 1, t.lastAccessedAt = :accessTime " +
           "WHERE t.taskId = :taskId")
    void incrementCacheHitCount(@Param("taskId") String taskId, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 更新相似度使用计数
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.similarityUsageCount = t.similarityUsageCount + 1 " +
           "WHERE t.taskId = :taskId")
    void incrementSimilarityUsageCount(@Param("taskId") String taskId);

    /**
     * 查找热门缓存任务（用于预热）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "AND t.cacheHitCount >= :minHitCount " +
           "ORDER BY t.cacheHitCount DESC, t.lastAccessedAt DESC")
    List<ModelTask> findPopularCacheTasks(@Param("minHitCount") int minHitCount, Pageable pageable);

    /**
     * 查找热门缓存任务（用于预热）- 分页版本
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "ORDER BY t.cacheHitCount DESC, t.accessCount DESC, t.lastAccessedAt DESC")
    Page<ModelTask> findHotCacheTasks(Pageable pageable);

    /**
     * 根据状态和缓存标志查找任务
     */
    List<ModelTask> findByStatusAndCachedTrue(ModelTask.TaskStatus status);

    /**
     * 统计已缓存的任务数量
     */
    long countByCachedTrue();

    /**
     * 根据小时模式查找任务（用于时间模式预热）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "AND HOUR(t.createdAt) = :hour " +
           "ORDER BY t.accessCount DESC, t.cacheHitCount DESC")
    List<ModelTask> findTasksByHourPattern(@Param("hour") int hour, Pageable pageable);

    /**
     * 查找最活跃用户的任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.cached = true AND t.status = 'COMPLETED' " +
           "AND t.clientIp IN (SELECT t2.clientIp FROM ModelTask t2 " +
           "GROUP BY t2.clientIp ORDER BY COUNT(t2) DESC) " +
           "ORDER BY t.accessCount DESC")
    List<ModelTask> findMostActiveUserTasks(Pageable pageable);

    /**
     * 查找最活跃的客户端IP
     */
    @Query("SELECT t.clientIp FROM ModelTask t WHERE t.status = 'COMPLETED' " +
           "AND t.createdAt >= :since " +
           "GROUP BY t.clientIp ORDER BY COUNT(t) DESC")
    List<String> findMostActiveClientIps(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 重载方法，使用默认时间范围
     */
    default List<String> findMostActiveClientIps(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return findMostActiveClientIps(since, pageable);
    }

    /**
     * 根据客户端IP查找最近任务
     */
    @Query("SELECT t FROM ModelTask t WHERE t.clientIp = :clientIp " +
           "AND t.cached = true AND t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt DESC")
    List<ModelTask> findRecentTasksByClientIp(@Param("clientIp") String clientIp, Pageable pageable);

    /**
     * 更新最后访问时间
     */
    @Modifying
    @Query("UPDATE ModelTask t SET t.lastAccessedAt = :lastAccessedAt WHERE t.taskId = :taskId")
    void updateLastAccessed(@Param("taskId") String taskId, @Param("lastAccessedAt") LocalDateTime lastAccessedAt);
}