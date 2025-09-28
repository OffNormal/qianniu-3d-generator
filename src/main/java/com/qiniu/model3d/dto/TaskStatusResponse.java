package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.entity.ModelPreviewImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务状态响应DTO
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskStatusResponse {

    private String taskId;
    private ModelTask.TaskStatus status;
    private Integer progress;
    private ModelResult result;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime completedAt;

    // 构造函数
    public TaskStatusResponse() {}

    public TaskStatusResponse(ModelTask task) {
        this.taskId = task.getTaskId();
        this.status = task.getStatus();
        this.progress = task.getProgress();
        this.errorMessage = task.getErrorMessage();
        this.createdAt = task.getCreatedAt();
        this.completedAt = task.getCompletedAt();

        // 如果任务完成，设置结果信息
        if (task.getStatus() == ModelTask.TaskStatus.COMPLETED && task.getModelId() != null) {
            this.result = new ModelResult();
            this.result.setModelId(task.getModelId());
            this.result.setDownloadUrl("/api/v1/models/download/" + task.getModelId());
            this.result.setPreviewUrl("/api/v1/models/preview/" + task.getModelId());
            
            // 设置多张预览图片信息
            if (task.getPreviewImages() != null && !task.getPreviewImages().isEmpty()) {
                List<PreviewImageInfo> previewImages = task.getPreviewImages().stream()
                    .map(img -> new PreviewImageInfo(
                        img.getId(),
                        "/api/v1/models/preview/" + task.getModelId() + "/" + img.getId(),
                        img.getImageOrder(),
                        img.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
                this.result.setPreviewImages(previewImages);
            }
            
            ModelInfo modelInfo = new ModelInfo();
            modelInfo.setVertices(task.getVerticesCount());
            modelInfo.setFaces(task.getFacesCount());
            modelInfo.setFileSize(formatFileSize(task.getFileSize()));
            modelInfo.setFormat(task.getOutputFormat().name().toLowerCase());
            this.result.setModelInfo(modelInfo);
        }
    }

    // 格式化文件大小
    private String formatFileSize(Long bytes) {
        if (bytes == null) return null;
        
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        }
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public ModelTask.TaskStatus getStatus() {
        return status;
    }

    public void setStatus(ModelTask.TaskStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public ModelResult getResult() {
        return result;
    }

    public void setResult(ModelResult result) {
        this.result = result;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * 模型结果内部类
     */
    public static class ModelResult {
        private String modelId;
        private String downloadUrl;
        private String previewUrl;
        private List<PreviewImageInfo> previewImages;
        private ModelInfo modelInfo;

        // Getters and Setters
        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
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

        public List<PreviewImageInfo> getPreviewImages() {
            return previewImages;
        }

        public void setPreviewImages(List<PreviewImageInfo> previewImages) {
            this.previewImages = previewImages;
        }

        public ModelInfo getModelInfo() {
            return modelInfo;
        }

        public void setModelInfo(ModelInfo modelInfo) {
            this.modelInfo = modelInfo;
        }
    }

    /**
     * 模型信息内部类
     */
    public static class ModelInfo {
        private Integer vertices;
        private Integer faces;
        private String fileSize;
        private String format;

        // Getters and Setters
        public Integer getVertices() {
            return vertices;
        }

        public void setVertices(Integer vertices) {
            this.vertices = vertices;
        }

        public Integer getFaces() {
            return faces;
        }

        public void setFaces(Integer faces) {
            this.faces = faces;
        }

        public String getFileSize() {
            return fileSize;
        }

        public void setFileSize(String fileSize) {
            this.fileSize = fileSize;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    /**
     * 预览图片信息内部类
     */
    public static class PreviewImageInfo {
        private Long id;
        private String url;
        private Integer order;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;

        public PreviewImageInfo() {}

        public PreviewImageInfo(Long id, String url, Integer order, LocalDateTime createdAt) {
            this.id = id;
            this.url = url;
            this.order = order;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}