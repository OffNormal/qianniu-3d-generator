package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    // 缓存相关字段
    @Column(name = "reference_count")
    private Integer referenceCount;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "access_count")
    private Integer accessCount;

    @Column(name = "file_signature", length = 64)
    private String fileSignature;

    @Column(name = "input_hash", length = 64)
    private String inputHash;
    
    @Column(name = "cached")
    private Boolean cached;
    
    @Column(name = "cache_hit_count")
    private Integer cacheHitCount;
    
    @Column(name = "similarity_usage_count")
    private Integer similarityUsageCount;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "obj_file_path", length = 500)
    private String objFilePath;
    
    @Column(name = "gltf_file_path", length = 500)
    private String gltfFilePath;
    
    @Column(name = "stl_file_path", length = 500)
    private String stlFilePath;

    // 关联的预览图片列表
    @OneToMany(mappedBy = "modelTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("imageOrder ASC")
    private List<ModelPreviewImage> previewImages;

    // 构造函数
    public ModelTask() {
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.progress = 0;
        this.referenceCount = 1;
        this.accessCount = 1;
        this.lastAccessed = LocalDateTime.now();
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

    public Integer getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(Integer referenceCount) {
        this.referenceCount = referenceCount;
    }

    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(LocalDateTime lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public String getFileSignature() {
        return fileSignature;
    }

    public void setFileSignature(String fileSignature) {
        this.fileSignature = fileSignature;
    }

    public String getInputHash() {
        return inputHash;
    }

    public void setInputHash(String inputHash) {
        this.inputHash = inputHash;
    }

    /**
     * 增加访问计数并更新最后访问时间
     */
    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }

    /**
     * 增加引用计数
     */
    public void incrementReferenceCount() {
        this.referenceCount = (this.referenceCount == null ? 0 : this.referenceCount) + 1;
    }

    /**
     * 减少引用计数
     */
    public void decrementReferenceCount() {
        this.referenceCount = Math.max(0, (this.referenceCount == null ? 0 : this.referenceCount) - 1);
    }
    
    public Boolean getCached() {
        return cached;
    }
    
    public void setCached(Boolean cached) {
        this.cached = cached;
    }
    
    public Integer getCacheHitCount() {
        return cacheHitCount;
    }
    
    public void setCacheHitCount(Integer cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }
    
    public Integer getSimilarityUsageCount() {
        return similarityUsageCount;
    }
    
    public void setSimilarityUsageCount(Integer similarityUsageCount) {
        this.similarityUsageCount = similarityUsageCount;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public String getObjFilePath() {
        return objFilePath;
    }
    
    public void setObjFilePath(String objFilePath) {
        this.objFilePath = objFilePath;
    }
    
    public String getGltfFilePath() {
        return gltfFilePath;
    }
    
    public void setGltfFilePath(String gltfFilePath) {
        this.gltfFilePath = gltfFilePath;
    }
    
    public String getStlFilePath() {
        return stlFilePath;
    }
    
    public void setStlFilePath(String stlFilePath) {
        this.stlFilePath = stlFilePath;
    }
    
    /**
     * 增加缓存命中计数
     */
    public void incrementCacheHitCount() {
        this.cacheHitCount = (this.cacheHitCount == null ? 0 : this.cacheHitCount) + 1;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * 增加相似度使用计数
     */
    public void incrementSimilarityUsageCount() {
        this.similarityUsageCount = (this.similarityUsageCount == null) ? 1 : this.similarityUsageCount + 1;
    }

    public List<ModelPreviewImage> getPreviewImages() {
        return previewImages;
    }

    public void setPreviewImages(List<ModelPreviewImage> previewImages) {
        this.previewImages = previewImages;
    }

    // 枚举类型定义
    public enum TaskType {
        TEXT, IMAGE
    }

    public enum Complexity {
        SIMPLE, MEDIUM, COMPLEX
    }

    public enum OutputFormat {
        OBJ, STL, PLY, GLB, USDZ, FBX, MP4
    }

    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    }
}