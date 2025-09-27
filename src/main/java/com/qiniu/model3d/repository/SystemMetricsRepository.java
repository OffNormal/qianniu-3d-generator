package com.qiniu.model3d.repository;

import com.qiniu.model3d.entity.SystemMetrics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 系统指标数据Repository接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetrics, Long> {

    /**
     * 根据指标日期查找记录
     */
    Optional<SystemMetrics> findByMetricDate(LocalDate metricDate);

    /**
     * 查找指定日期范围内的指标记录
     */
    @Query("SELECT sm FROM SystemMetrics sm WHERE sm.metricDate BETWEEN :startDate AND :endDate ORDER BY sm.metricDate")
    List<SystemMetrics> findByMetricDateBetween(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);

    /**
     * 查找最近N天的指标记录
     */
    @Query("SELECT sm FROM SystemMetrics sm ORDER BY sm.metricDate DESC")
    List<SystemMetrics> findRecentMetrics(Pageable pageable);

    /**
     * 查找最新的指标记录
     */
    Optional<SystemMetrics> findFirstByOrderByMetricDateDesc();

    /**
     * 统计指定日期范围内的总任务数
     */
    @Query("SELECT SUM(sm.totalTasks) FROM SystemMetrics sm WHERE sm.metricDate BETWEEN :startDate AND :endDate")
    Long getTotalTasksByDateRange(@Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);

    /**
     * 统计指定日期范围内的成功任务数
     */
    @Query("SELECT SUM(sm.successTasks) FROM SystemMetrics sm WHERE sm.metricDate BETWEEN :startDate AND :endDate")
    Long getSuccessTasksByDateRange(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

    /**
     * 计算指定日期范围内的平均成功率
     */
    @Query("SELECT AVG(sm.successRate) FROM SystemMetrics sm WHERE sm.metricDate BETWEEN :startDate AND :endDate")
    Double getAvgSuccessRateByDateRange(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    /**
     * 计算指定日期范围内的平均评分
     */
    @Query("SELECT AVG(sm.avgRating) FROM SystemMetrics sm WHERE sm.metricDate BETWEEN :startDate AND :endDate AND sm.avgRating > 0")
    Double getAvgRatingByDateRange(@Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate);

    /**
     * 查找成功率低于阈值的日期
     */
    @Query("SELECT sm FROM SystemMetrics sm WHERE sm.successRate < :threshold ORDER BY sm.metricDate DESC")
    List<SystemMetrics> findLowSuccessRateMetrics(@Param("threshold") Double threshold);

    /**
     * 查找评分低于阈值的日期
     */
    @Query("SELECT sm FROM SystemMetrics sm WHERE sm.avgRating < :threshold AND sm.avgRating > 0 ORDER BY sm.metricDate DESC")
    List<SystemMetrics> findLowRatingMetrics(@Param("threshold") Double threshold);

    /**
     * 删除指定日期之前的旧数据
     */
    @Query("DELETE FROM SystemMetrics sm WHERE sm.metricDate < :cutoffDate")
    void deleteOldMetrics(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 删除指定日期之前的旧数据（返回删除数量）
     */
    @Query("DELETE FROM SystemMetrics sm WHERE sm.metricDate < :cutoffDate")
    @Modifying
    int deleteOldData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 检查指定日期是否已有数据
     */
    boolean existsByMetricDate(LocalDate metricDate);

    /**
     * 获取指定月份的指标数据
     */
    @Query("SELECT sm FROM SystemMetrics sm WHERE YEAR(sm.metricDate) = :year AND MONTH(sm.metricDate) = :month ORDER BY sm.metricDate")
    List<SystemMetrics> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 获取指定年份的指标数据
     */
    @Query("SELECT sm FROM SystemMetrics sm WHERE YEAR(sm.metricDate) = :year ORDER BY sm.metricDate")
    List<SystemMetrics> findByYear(@Param("year") int year);
}