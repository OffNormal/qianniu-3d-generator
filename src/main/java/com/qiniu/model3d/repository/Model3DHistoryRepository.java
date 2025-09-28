package com.qiniu.model3d.repository;

import com.qiniu.model3d.entity.Model3DHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 3D模型历史记录数据访问接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Repository
public interface Model3DHistoryRepository extends JpaRepository<Model3DHistory, Long> {

    /**
     * 根据模型名称查找历史记录（模糊匹配）
     */
    Page<Model3DHistory> findByModelNameContainingIgnoreCase(String modelName, Pageable pageable);

    /**
     * 根据创建时间范围查找历史记录
     */
    Page<Model3DHistory> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据状态查找历史记录
     */
    Page<Model3DHistory> findByStatus(String status, Pageable pageable);

    /**
     * 根据客户端IP查找历史记录
     */
    Page<Model3DHistory> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 根据原始任务ID查找历史记录
     */
    Optional<Model3DHistory> findByOriginalTaskId(String originalTaskId);

    /**
     * 查找最近的历史记录
     */
    List<Model3DHistory> findTop10ByOrderByCreateTimeDesc();

    /**
     * 根据模型名称和描述进行搜索（模糊匹配）
     */
    @Query("SELECT h FROM Model3DHistory h WHERE " +
           "(LOWER(h.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.inputText) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY h.createTime DESC")
    Page<Model3DHistory> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计总记录数
     */
    @Query("SELECT COUNT(h) FROM Model3DHistory h")
    long countAllRecords();

    /**
     * 统计总文件大小
     */
    @Query("SELECT COALESCE(SUM(h.fileSize), 0) FROM Model3DHistory h WHERE h.fileSize IS NOT NULL")
    long getTotalFileSize();

    /**
     * 统计总下载次数
     */
    @Query("SELECT COALESCE(SUM(h.downloadCount), 0) FROM Model3DHistory h WHERE h.downloadCount IS NOT NULL")
    long getTotalDownloadCount();

    /**
     * 获取最受欢迎的模型（按下载次数排序）
     */
    @Query("SELECT h FROM Model3DHistory h WHERE h.downloadCount > 0 ORDER BY h.downloadCount DESC")
    Page<Model3DHistory> findMostPopularModels(Pageable pageable);

    /**
     * 根据文件大小范围查找
     */
    @Query("SELECT h FROM Model3DHistory h WHERE h.fileSize BETWEEN :minSize AND :maxSize ORDER BY h.createTime DESC")
    Page<Model3DHistory> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize, Pageable pageable);

    /**
     * 更新下载次数
     */
    @Modifying
    @Transactional
    @Query("UPDATE Model3DHistory h SET h.downloadCount = h.downloadCount + 1 WHERE h.id = :id")
    int incrementDownloadCount(@Param("id") Long id);

    /**
     * 删除指定时间之前的记录
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Model3DHistory h WHERE h.createTime < :cutoffTime")
    int deleteOldRecords(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查找有文件数据的记录
     */
    @Query("SELECT h FROM Model3DHistory h WHERE h.modelFileData IS NOT NULL ORDER BY h.createTime DESC")
    Page<Model3DHistory> findRecordsWithFileData(Pageable pageable);

    /**
     * 查找有文件路径的记录
     */
    @Query("SELECT h FROM Model3DHistory h WHERE h.modelFilePath IS NOT NULL AND h.modelFilePath != '' ORDER BY h.createTime DESC")
    Page<Model3DHistory> findRecordsWithFilePath(Pageable pageable);

    /**
     * 根据模型格式查找
     */
    Page<Model3DHistory> findByModelFormat(String modelFormat, Pageable pageable);

    /**
     * 查找今天创建的记录
     */
    @Query("SELECT h FROM Model3DHistory h WHERE DATE(h.createTime) = CURRENT_DATE ORDER BY h.createTime DESC")
    List<Model3DHistory> findTodayRecords();

    /**
     * 查找本周创建的记录
     */
    @Query("SELECT h FROM Model3DHistory h WHERE h.createTime >= :weekStart ORDER BY h.createTime DESC")
    List<Model3DHistory> findThisWeekRecords(@Param("weekStart") LocalDateTime weekStart);

    /**
     * 查找本月创建的记录
     */
    @Query("SELECT h FROM Model3DHistory h WHERE YEAR(h.createTime) = YEAR(CURRENT_DATE) AND MONTH(h.createTime) = MONTH(CURRENT_DATE) ORDER BY h.createTime DESC")
    List<Model3DHistory> findThisMonthRecords();

    /**
     * 复杂搜索：支持多条件组合
     */
    @Query("SELECT h FROM Model3DHistory h WHERE " +
           "(:modelName IS NULL OR LOWER(h.modelName) LIKE LOWER(CONCAT('%', :modelName, '%'))) AND " +
           "(:status IS NULL OR h.status = :status) AND " +
           "(:modelFormat IS NULL OR h.modelFormat = :modelFormat) AND " +
           "(:startTime IS NULL OR h.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR h.createTime <= :endTime) " +
           "ORDER BY h.createTime DESC")
    Page<Model3DHistory> findByMultipleConditions(
            @Param("modelName") String modelName,
            @Param("status") String status,
            @Param("modelFormat") String modelFormat,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}