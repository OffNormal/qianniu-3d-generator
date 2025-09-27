package com.qiniu.model3d.repository;

import com.qiniu.model3d.entity.TaskEvaluation;
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
 * 任务评估数据Repository接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Repository
public interface TaskEvaluationRepository extends JpaRepository<TaskEvaluation, Long> {

    /**
     * 根据jobId查找任务评估记录
     */
    Optional<TaskEvaluation> findByJobId(String jobId);

    /**
     * 查找指定时间范围内的评估记录
     */
    @Query("SELECT te FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    List<TaskEvaluation> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的总任务数
     */
    @Query("SELECT COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的成功任务数
     */
    @Query("SELECT COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.status = 'DONE'")
    Long countSuccessTasksByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的失败任务数
     */
    @Query("SELECT COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.status = 'FAIL'")
    Long countFailedTasksByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 计算指定时间范围内的平均执行时间
     */
    @Query("SELECT AVG(te.durationSeconds) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.status = 'DONE'")
    Double getAvgDurationByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 计算指定时间范围内的平均评分
     */
    @Query("SELECT AVG(te.userRating) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.userRating IS NOT NULL")
    Double getAvgRatingByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的总下载次数
     */
    @Query("SELECT SUM(te.downloadCount) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalDownloadsByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的总预览次数
     */
    @Query("SELECT SUM(te.previewCount) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalPreviewsByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的独立用户数（基于IP）
     */
    @Query("SELECT COUNT(DISTINCT te.clientIp) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long countDistinctUsersByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取格式分布统计
     */
    @Query("SELECT te.resultFormat, COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime GROUP BY te.resultFormat")
    List<Object[]> getFormatDistributionByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 获取评分分布统计
     */
    @Query("SELECT te.userRating, COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.userRating IS NOT NULL GROUP BY te.userRating")
    List<Object[]> getRatingDistributionByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 获取热门提示词（按使用频率排序）
     */
    @Query("SELECT te.prompt, COUNT(te) as cnt FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.prompt IS NOT NULL GROUP BY te.prompt ORDER BY cnt DESC")
    List<Object[]> getPopularPromptsByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查找有用户评分的记录
     */
    @Query("SELECT te FROM TaskEvaluation te WHERE te.userRating IS NOT NULL ORDER BY te.createdAt DESC")
    List<TaskEvaluation> findTasksWithRating();

    /**
     * 查找最近的评估记录
     */
    @Query("SELECT te FROM TaskEvaluation te ORDER BY te.createdAt DESC")
    List<TaskEvaluation> findRecentTasks();
    
    /**
     * 获取热门提示词（按使用次数排序）
     */
    @Query("SELECT t.prompt, COUNT(t) as promptCount FROM TaskEvaluation t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.prompt ORDER BY promptCount DESC")
    List<Object[]> getPopularPrompts(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate, 
                                   Pageable pageable);
    
    /**
     * 根据状态查询任务（分页）
     */
    Page<TaskEvaluation> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据状态和创建时间范围查询任务（分页）
     */
    Page<TaskEvaluation> findByStatusAndCreatedAtBetween(String status, 
                                                        LocalDateTime startDate, 
                                                        LocalDateTime endDate, 
                                                        Pageable pageable);
    
    /**
     * 根据创建时间范围查询任务（分页）
     */
    Page<TaskEvaluation> findByCreatedAtBetween(LocalDateTime startDate, 
                                               LocalDateTime endDate, 
                                               Pageable pageable);
    
    // 为MetricsScheduler和AdminDashboardController添加的方法
    
    /**
     * 统计指定时间范围内的任务数
     */
    @Query("SELECT COUNT(te) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long countTasksByDateRange(@Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内指定状态的任务数
     */
    @Query("SELECT COUNT(te) FROM TaskEvaluation te WHERE te.status = :status AND te.createdAt BETWEEN :startTime AND :endTime")
    Long countTasksByStatusAndDateRange(@Param("status") String status,
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算指定时间范围内的平均执行时间
     */
    @Query("SELECT AVG(te.durationSeconds) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.status = 'DONE'")
    Double getAverageDurationByDateRange(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的独立用户数（基于IP）
     */
    @Query("SELECT COUNT(DISTINCT te.clientIp) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long countDistinctUsersByDateRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算指定时间范围内的平均评分
     */
    @Query("SELECT AVG(te.userRating) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime AND te.userRating IS NOT NULL")
    Double getAverageRatingByDateRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的总下载次数
     */
    @Query("SELECT SUM(te.downloadCount) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalDownloadsByDateRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的总预览次数
     */
    @Query("SELECT SUM(te.previewCount) FROM TaskEvaluation te WHERE te.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalPreviewsByDateRange(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime);
}