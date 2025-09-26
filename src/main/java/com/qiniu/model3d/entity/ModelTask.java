package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 3D模型生成任务实体类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Entity
@Table(name = "model_tasks")
public class ModelTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, nullable = false, length = 50)
    private String taskId;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    @Column(name = "input_image_path", length = 500)
    private String inputImagePath;

    @Column(name = "complexity", length = 20)
    @Enumerated(EnumType.STRING)
    private Complexity complexity;

    @Column(name = "output_format", length = 10)
    @Enumerated(EnumType.STRING)
    private OutputFormat outputFormat;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "model_id", length = 50)
    private String modelId;

    @Column(name = "model_file_path", length = 500)
    private String modelFilePath;

    @Column(name = "preview_image_path", length = 500)
    private String previewImagePath;

    @Column(name = "vertices_count")
    private Integer verticesCount;

    @Column(name = "faces_count")
    private Integer facesCount;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // 构造函数
    public ModelTask() {
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.progress = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getInputImagePath() {
        return inputImagePath;
    }

    public void setInputImagePath(String inputImagePath) {
        this.inputImagePath = inputImagePath;
    }

    public Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(Complexity complexity) {
        this.complexity = complexity;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }

    public Integer getVerticesCount() {
        return verticesCount;
    }

    public void setVerticesCount(Integer verticesCount) {
        this.verticesCount = verticesCount;
    }

    public Integer getFacesCount() {
        return facesCount;
    }

    public void setFacesCount(Integer facesCount) {
        this.facesCount = facesCount;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    // 枚举类型定义
    public enum TaskType {
        TEXT, IMAGE
    }

    public enum Complexity {
        SIMPLE, MEDIUM, COMPLEX
    }

    public enum OutputFormat {
        OBJ, STL, PLY
    }

    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    }
}