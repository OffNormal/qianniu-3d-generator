package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 3D模型历史记录实体类
 * 用于存储和管理用户生成的3D模型历史记录
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Entity
@Table(name = "model_3d_history")
public class Model3DHistory implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "model_file_path", length = 500)
    private String modelFilePath;

    @Lob
    @Column(name = "model_file_data")
    private byte[] modelFileData;

    @Column(name = "preview_image_path", length = 500)
    private String previewImagePath;

    @Lob
    @Column(name = "preview_image_data")
    private byte[] previewImageData;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    @Column(name = "input_image_path", length = 500)
    private String inputImagePath;

    @Column(name = "model_format", length = 10)
    private String modelFormat;

    @Column(name = "vertices_count")
    private Integer verticesCount;

    @Column(name = "faces_count")
    private Integer facesCount;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "generation_time_seconds")
    private Integer generationTimeSeconds;

    @Column(name = "complexity", length = 20)
    private String complexity;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "original_task_id", length = 50)
    private String originalTaskId;

    // 构造函数
    public Model3DHistory() {
        this.createTime = LocalDateTime.now();
        this.downloadCount = 0;
        this.status = "COMPLETED";
    }

    public Model3DHistory(String modelName, String description) {
        this();
        this.modelName = modelName;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public byte[] getModelFileData() {
        return modelFileData;
    }

    public void setModelFileData(byte[] modelFileData) {
        this.modelFileData = modelFileData;
    }

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }

    public byte[] getPreviewImageData() {
        return previewImageData;
    }

    public void setPreviewImageData(byte[] previewImageData) {
        this.previewImageData = previewImageData;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getModelFormat() {
        return modelFormat;
    }

    public void setModelFormat(String modelFormat) {
        this.modelFormat = modelFormat;
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

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getGenerationTimeSeconds() {
        return generationTimeSeconds;
    }

    public void setGenerationTimeSeconds(Integer generationTimeSeconds) {
        this.generationTimeSeconds = generationTimeSeconds;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalTaskId() {
        return originalTaskId;
    }

    public void setOriginalTaskId(String originalTaskId) {
        this.originalTaskId = originalTaskId;
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        if (this.downloadCount == null) {
            this.downloadCount = 0;
        }
        this.downloadCount++;
    }

    /**
     * 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "未知";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * 检查是否有模型文件数据
     */
    public boolean hasModelFileData() {
        return modelFileData != null && modelFileData.length > 0;
    }

    /**
     * 检查是否有预览图数据
     */
    public boolean hasPreviewImageData() {
        return previewImageData != null && previewImageData.length > 0;
    }

    @Override
    public String toString() {
        return "Model3DHistory{" +
                "id=" + id +
                ", modelName='" + modelName + '\'' +
                ", fileSize=" + fileSize +
                ", createTime=" + createTime +
                ", downloadCount=" + downloadCount +
                '}';
    }
}