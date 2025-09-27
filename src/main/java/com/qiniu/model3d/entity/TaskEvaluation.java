package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 任务评估记录实体类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Entity
@Table(name = "task_evaluation")
public class TaskEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 100)
    private String jobId;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "result_format", length = 20)
    private String resultFormat;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "user_rating")
    private Integer userRating;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "preview_count")
    private Integer previewCount = 0;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public TaskEvaluation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.downloadCount = 0;
        this.previewCount = 0;
    }

    public TaskEvaluation(String jobId, String prompt, String resultFormat, String status) {
        this();
        this.jobId = jobId;
        this.prompt = prompt;
        this.resultFormat = resultFormat;
        this.status = status;
        this.submitTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
        if (this.submitTime != null && completeTime != null) {
            this.durationSeconds = (int) java.time.Duration.between(this.submitTime, completeTime).getSeconds();
        }
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getFileSizeKb() {
        return fileSizeKb;
    }

    public void setFileSizeKb(Integer fileSizeKb) {
        this.fileSizeKb = fileSizeKb;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    public Integer getPreviewCount() {
        return previewCount;
    }

    public void setPreviewCount(Integer previewCount) {
        this.previewCount = previewCount;
    }

    public void incrementPreviewCount() {
        this.previewCount = (this.previewCount == null ? 0 : this.previewCount) + 1;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TaskEvaluation{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", status='" + status + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", userRating=" + userRating +
                ", downloadCount=" + downloadCount +
                ", previewCount=" + previewCount +
                '}';
    }
}