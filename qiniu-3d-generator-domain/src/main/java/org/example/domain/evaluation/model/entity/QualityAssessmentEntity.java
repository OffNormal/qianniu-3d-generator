package org.example.domain.evaluation.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 质量评估实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAssessmentEntity {
    
    /** 评估ID */
    private String assessmentId;
    
    /** 任务ID */
    private String taskId;
    
    /** 几何质量评分 (0-100) */
    private Double geometryScore;
    
    /** 视觉质量评分 (0-100) */
    private Double visualScore;
    
    /** 功能指标评分 (0-100) */
    private Double functionalScore;
    
    /** 综合评分 (0-100) */
    private Double overallScore;
    
    /** 面数 */
    private Integer faceCount;
    
    /** 顶点数 */
    private Integer vertexCount;
    
    /** 文件大小 (KB) */
    private Long fileSizeKb;
    
    /** 纹理分辨率 */
    private String textureResolution;
    
    /** 处理时间 (秒) */
    private Long processingTimeSeconds;
    
    /** 详细评估结果 (JSON) */
    private String detailResults;
    
    /** 评估时间 */
    private LocalDateTime assessmentTime;
    
    /**
     * 计算综合评分
     */
    public void calculateOverallScore() {
        if (geometryScore != null && visualScore != null && functionalScore != null) {
            // 权重：几何质量30%，视觉质量40%，功能指标30%
            this.overallScore = geometryScore * 0.3 + visualScore * 0.4 + functionalScore * 0.3;
        }
    }
    
    /**
     * 评估几何质量
     */
    public void assessGeometry(Integer faceCount, Integer vertexCount) {
        this.faceCount = faceCount;
        this.vertexCount = vertexCount;
        
        double score = 100.0;
        
        // 面数合理性评估 (5000-15000为最佳范围)
        if (faceCount < 1000) {
            score -= 30; // 面数过少，细节不足
        } else if (faceCount > 20000) {
            score -= 20; // 面数过多，性能影响
        }
        
        // 拓扑结构评估 (顶点与面数比例)
        if (faceCount > 0) {
            double ratio = (double) vertexCount / faceCount;
            if (ratio < 0.5 || ratio > 0.8) {
                score -= 15; // 拓扑结构不合理
            }
        }
        
        this.geometryScore = Math.max(0, Math.min(100, score));
    }
    
    /**
     * 评估功能指标
     */
    public void assessFunctional(Long processingTime, Long fileSize) {
        this.processingTimeSeconds = processingTime;
        this.fileSizeKb = fileSize;
        
        double score = 100.0;
        
        // 处理时间评估 (30秒内为优秀)
        if (processingTime > 120) {
            score -= 40; // 超过2分钟
        } else if (processingTime > 60) {
            score -= 20; // 超过1分钟
        }
        
        // 文件大小评估 (1-10MB为合理范围)
        long fileSizeMb = fileSize / 1024;
        if (fileSizeMb > 20) {
            score -= 30; // 文件过大
        } else if (fileSizeMb < 0.5) {
            score -= 20; // 文件过小，可能质量不足
        }
        
        this.functionalScore = Math.max(0, Math.min(100, score));
    }
    
    /**
     * 获取质量等级
     */
    public String getQualityLevel() {
        if (overallScore == null) {
            return "未评估";
        }
        
        if (overallScore >= 90) {
            return "优秀";
        } else if (overallScore >= 80) {
            return "良好";
        } else if (overallScore >= 70) {
            return "一般";
        } else if (overallScore >= 60) {
            return "较差";
        } else {
            return "差";
        }
    }
}