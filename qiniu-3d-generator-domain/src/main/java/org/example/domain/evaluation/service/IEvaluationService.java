package org.example.domain.evaluation.service;

import org.example.domain.evaluation.model.entity.QualityAssessmentEntity;
import org.example.domain.generation.model.entity.GenerationTaskEntity;

import java.util.List;

/**
 * 评估服务接口
 */
public interface IEvaluationService {
    
    /**
     * 评估3D模型质量
     * @param taskEntity 生成任务实体
     * @return 质量评估结果
     */
    QualityAssessmentEntity assessQuality(GenerationTaskEntity taskEntity);
    
    /**
     * 获取评估报告
     * @param taskId 任务ID
     * @return 评估实体
     */
    QualityAssessmentEntity getAssessment(String taskId);
    
    /**
     * 获取质量统计
     * @param userId 用户ID
     * @param days 统计天数
     * @return 统计结果
     */
    QualityStatistics getQualityStatistics(String userId, int days);
    
    /**
     * 质量统计结果
     */
    class QualityStatistics {
        private double averageScore;
        private int totalTasks;
        private int excellentCount;
        private int goodCount;
        private int averageCount;
        private int poorCount;
        private double successRate;
        
        // getters and setters
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        
        public int getExcellentCount() { return excellentCount; }
        public void setExcellentCount(int excellentCount) { this.excellentCount = excellentCount; }
        
        public int getGoodCount() { return goodCount; }
        public void setGoodCount(int goodCount) { this.goodCount = goodCount; }
        
        public int getAverageCount() { return averageCount; }
        public void setAverageCount(int averageCount) { this.averageCount = averageCount; }
        
        public int getPoorCount() { return poorCount; }
        public void setPoorCount(int poorCount) { this.poorCount = poorCount; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}