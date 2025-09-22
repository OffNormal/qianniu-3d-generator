package org.example.infrastructure.persistent.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 生成任务持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationTaskPO {
    
    /** 任务ID */
    private String taskId;
    
    /** 用户ID */
    private String userId;
    
    /** 生成类型 */
    private String generationType;
    
    /** 输入内容 */
    private String inputContent;
    
    /** 输入内容哈希 */
    private String inputHash;
    
    /** 任务状态 */
    private String status;
    
    /** 生成结果URL */
    private String resultUrl;
    
    /** 模型文件路径 */
    private String modelFilePath;
    
    /** 预览图URL */
    private String previewImageUrl;
    
    /** 第三方API任务ID */
    private String externalTaskId;
    
    /** 生成参数 */
    private String generationParams;
    
    /** 质量评分 */
    private Double qualityScore;
    
    /** 用户评分 */
    private Integer userRating;
    
    /** 处理耗时 */
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
}