package org.example.domain.generation.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;

import java.time.LocalDateTime;

/**
 * 3D模型生成任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationTaskEntity {
    
    /** 任务ID */
    private String taskId;
    
    /** 用户ID */
    private String userId;
    
    /** 生成类型 */
    private GenerationType generationType;
    
    /** 输入内容（文本描述或图片URL） */
    private String inputContent;
    
    /** 输入内容哈希（用于缓存匹配） */
    private String inputHash;
    
    /** 任务状态 */
    private TaskStatus status;
    
    /** 生成结果URL */
    private String resultUrl;
    
    /** 模型文件路径 */
    private String modelFilePath;
    
    /** 预览图URL */
    private String previewImageUrl;
    
    /** 第三方API任务ID */
    private String externalTaskId;
    
    /** API提供商 */
    private String apiProvider;
    
    /** 生成参数（JSON格式） */
    private String generationParams;
    
    /** 质量评分 */
    private Double qualityScore;
    
    /** 用户评分 */
    private Integer userRating;
    
    /** 处理耗时（秒） */
    private Long processingTime;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 是否来自缓存 */
    private Boolean fromCache;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /** 完成时间 */
    private LocalDateTime completeTime;
    
    /**
     * 开始处理
     */
    public void startProcessing() {
        this.status = TaskStatus.PROCESSING;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 完成处理
     */
    public void completeProcessing(String resultUrl, String modelFilePath, String previewImageUrl) {
        this.status = TaskStatus.COMPLETED;
        this.resultUrl = resultUrl;
        this.modelFilePath = modelFilePath;
        this.previewImageUrl = previewImageUrl;
        this.completeTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        
        // 计算处理耗时
        if (this.createTime != null) {
            this.processingTime = java.time.Duration.between(this.createTime, this.completeTime).getSeconds();
        }
    }
    
    /**
     * 处理失败
     */
    public void failProcessing(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completeTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 设置缓存命中
     */
    public void setCacheHit(String resultUrl, String modelFilePath, String previewImageUrl) {
        this.status = TaskStatus.CACHED;
        this.resultUrl = resultUrl;
        this.modelFilePath = modelFilePath;
        this.previewImageUrl = previewImageUrl;
        this.fromCache = true;
        this.completeTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.processingTime = 0L; // 缓存命中耗时为0
    }
    
    /**
     * 更新质量评分
     */
    public void updateQualityScore(Double score) {
        this.qualityScore = score;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 更新用户评分
     */
    public void updateUserRating(Integer rating) {
        this.userRating = rating;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return TaskStatus.COMPLETED.equals(this.status) || TaskStatus.CACHED.equals(this.status);
    }
    
    /**
     * 是否失败
     */
    public boolean isFailed() {
        return TaskStatus.FAILED.equals(this.status);
    }
}