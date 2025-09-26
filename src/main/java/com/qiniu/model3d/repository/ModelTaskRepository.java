package com.qiniu.model3d.repository;

import com.qiniu.model3d.entity.ModelTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 查找最近的任务（用于历史记录）
     */
    @Query("SELECT t FROM ModelTask t WHERE t.clientIp = :clientIp AND t.status = 'COMPLETED' ORDER BY t.completedAt DESC")
    List<ModelTask> findRecentCompletedTasks(@Param("clientIp") String clientIp, Pageable pageable);
}