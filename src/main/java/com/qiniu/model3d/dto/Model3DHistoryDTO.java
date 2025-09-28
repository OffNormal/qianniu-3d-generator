package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 3D模型历史记录数据传输对象
 * 用于前后端数据传输，不包含大文件数据
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Model3DHistoryDTO {

    private Long id;
    private String modelName;
    private String modelFilePath;
    private String previewImagePath;
    private Long fileSize;
    private String formattedFileSize;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    private String description;
    private String inputText;
    private String inputImagePath;
    private String modelFormat;
    private Integer verticesCount;
    private Integer facesCount;
    private Integer downloadCount;
    private String clientIp;
    private Integer generationTimeSeconds;
    private String complexity;
    private String status;
    private String originalTaskId;
    
    // 额外的显示字段
    private boolean hasModelFile;
    private boolean hasPreviewImage;
    private String downloadUrl;
    private String previewUrl;

    // 构造函数
    public Model3DHistoryDTO() {}

    public Model3DHistoryDTO(Long id, String modelName, String description) {
        this.id = id;
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

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public void setPreviewImagePath(String previewImagePath) {
        this.previewImagePath = previewImagePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public void setFormattedFileSize(String formattedFileSize) {
        this.formattedFileSize = formattedFileSize;
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

    public boolean isHasModelFile() {
        return hasModelFile;
    }

    public void setHasModelFile(boolean hasModelFile) {
        this.hasModelFile = hasModelFile;
    }

    public boolean isHasPreviewImage() {
        return hasPreviewImage;
    }

    public void setHasPreviewImage(boolean hasPreviewImage) {
        this.hasPreviewImage = hasPreviewImage;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    @Override
    public String toString() {
        return "Model3DHistoryDTO{" +
                "id=" + id +
                ", modelName='" + modelName + '\'' +
                ", fileSize=" + fileSize +
                ", createTime=" + createTime +
                ", downloadCount=" + downloadCount +
                '}';
    }
}