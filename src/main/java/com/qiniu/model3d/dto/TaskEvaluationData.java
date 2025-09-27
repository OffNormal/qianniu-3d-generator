package com.qiniu.model3d.dto;

import java.time.LocalDateTime;

/**
 * 任务评估数据传输对象
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public class TaskEvaluationData {

    private String jobId;
    private String prompt;
    private String resultFormat;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime completeTime;
    private Integer durationSeconds;
    private Integer fileSizeKb;
    private String clientIp;
    private String errorMessage;

    // 构造函数
    public TaskEvaluationData() {
    }

    public TaskEvaluationData(String jobId, String prompt, String resultFormat, String status) {
        this.jobId = jobId;
        this.prompt = prompt;
        this.resultFormat = resultFormat;
        this.status = status;
        this.submitTime = LocalDateTime.now();
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "TaskEvaluationData{" +
                "jobId='" + jobId + '\'' +
                ", status='" + status + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", fileSizeKb=" + fileSizeKb +
                '}';
    }
}