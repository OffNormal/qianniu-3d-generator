package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统指标统计实体类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Entity
@Table(name = "system_metrics")
public class SystemMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;

    @Column(name = "total_tasks")
    private Integer totalTasks = 0;

    @Column(name = "success_tasks")
    private Integer successTasks = 0;

    @Column(name = "failed_tasks")
    private Integer failedTasks = 0;

    @Column(name = "avg_duration_seconds", precision = 10, scale = 2)
    private BigDecimal avgDurationSeconds = BigDecimal.ZERO;

    @Column(name = "total_users")
    private Integer totalUsers = 0;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_downloads")
    private Integer totalDownloads = 0;

    @Column(name = "total_previews")
    private Integer totalPreviews = 0;

    @Column(name = "success_rate", precision = 5, scale = 4)
    private BigDecimal successRate = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 构造函数
    public SystemMetrics() {
        this.createdAt = LocalDateTime.now();
    }

    public SystemMetrics(LocalDate metricDate) {
        this();
        this.metricDate = metricDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getSuccessTasks() {
        return successTasks;
    }

    public void setSuccessTasks(Integer successTasks) {
        this.successTasks = successTasks;
        calculateSuccessRate();
    }

    public Integer getFailedTasks() {
        return failedTasks;
    }

    public void setFailedTasks(Integer failedTasks) {
        this.failedTasks = failedTasks;
        calculateSuccessRate();
    }

    public BigDecimal getAvgDurationSeconds() {
        return avgDurationSeconds;
    }

    public void setAvgDurationSeconds(BigDecimal avgDurationSeconds) {
        this.avgDurationSeconds = avgDurationSeconds;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(Integer totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public Integer getTotalPreviews() {
        return totalPreviews;
    }

    public void setTotalPreviews(Integer totalPreviews) {
        this.totalPreviews = totalPreviews;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 计算成功率
    public void calculateSuccessRate() {
        if (totalTasks != null && totalTasks > 0) {
            int success = successTasks != null ? successTasks : 0;
            this.successRate = BigDecimal.valueOf(success)
                    .divide(BigDecimal.valueOf(totalTasks), 4, BigDecimal.ROUND_HALF_UP);
        } else {
            this.successRate = BigDecimal.ZERO;
        }
    }

    // 计算下载率
    public BigDecimal getDownloadRate() {
        if (successTasks != null && successTasks > 0) {
            int downloads = totalDownloads != null ? totalDownloads : 0;
            return BigDecimal.valueOf(downloads)
                    .divide(BigDecimal.valueOf(successTasks), 4, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // 更新总任务数
    public void updateTotalTasks() {
        int success = successTasks != null ? successTasks : 0;
        int failed = failedTasks != null ? failedTasks : 0;
        this.totalTasks = success + failed;
        calculateSuccessRate();
    }

    @Override
    public String toString() {
        return "SystemMetrics{" +
                "id=" + id +
                ", metricDate=" + metricDate +
                ", totalTasks=" + totalTasks +
                ", successTasks=" + successTasks +
                ", failedTasks=" + failedTasks +
                ", successRate=" + successRate +
                ", avgRating=" + avgRating +
                '}';
    }
}