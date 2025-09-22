package org.example.domain.evaluation.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.evaluation.model.entity.QualityAssessmentEntity;
import org.example.domain.evaluation.service.IEvaluationService;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 质量评估服务实现
 */
@Slf4j
@Service
public class EvaluationServiceImpl implements IEvaluationService {
    
    @Override
    public QualityAssessmentEntity assessQuality(GenerationTaskEntity taskEntity) {
        try {
            String taskId = taskEntity.getTaskId();
            String resultUrl = taskEntity.getResultUrl();
            
            log.info("开始质量评估: taskId={}, resultUrl={}", taskId, resultUrl);
            
            QualityAssessmentEntity assessment = new QualityAssessmentEntity();
            assessment.setAssessmentId("assessment_" + taskId);
            assessment.setTaskId(taskId);
            
            // 执行各项质量评估
            double geometryScore = evaluateGeometry(resultUrl);
            double textureScore = evaluateTexture(resultUrl);
            double functionalityScore = evaluateFunctionality(resultUrl);
            
            // 设置评估分数
            assessment.setGeometryScore(geometryScore);
            assessment.setVisualScore(textureScore);
            assessment.setFunctionalScore(functionalityScore);
            
            // 计算综合评分
            assessment.calculateOverallScore();
            
            // 分析模型统计信息
            ModelStats stats = analyzeModelStats(resultUrl);
            assessment.setFaceCount(stats.getFaceCount());
            assessment.setVertexCount(stats.getVertexCount());
            assessment.setFileSizeKb(stats.getFileSize() / 1024);
            assessment.setProcessingTimeSeconds(30L);
            assessment.setAssessmentTime(LocalDateTime.now());
            
            log.info("质量评估完成，任务ID: {}, 综合评分: {}", taskEntity.getTaskId(), assessment.getOverallScore());
            
            return assessment;
            
        } catch (Exception e) {
            log.error("质量评估失败: taskId={}", taskEntity.getTaskId(), e);
            throw new RuntimeException("质量评估失败", e);
        }
    }
    
    @Override
    public QualityAssessmentEntity getAssessment(String taskId) {
        try {
            log.info("获取质量评估结果: taskId={}", taskId);
            
            // 这里应该从数据库查询评估结果
            // 暂时返回模拟数据
            return QualityAssessmentEntity.builder()
                    .assessmentId("assessment_" + taskId)
                    .taskId(taskId)
                    .geometryScore(8.5)
                    .visualScore(7.8)
                    .functionalScore(8.0)
                    .overallScore(8.1)
                    .faceCount(5000)
                    .vertexCount(2500)
                    .fileSizeKb(1024L)
                    .processingTimeSeconds(30L)
                    .assessmentTime(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("获取质量评估结果失败: taskId={}", taskId, e);
            throw new RuntimeException("获取质量评估结果失败", e);
        }
    }
    
    @Override
    public IEvaluationService.QualityStatistics getQualityStatistics(String userId, int days) {
        // 这里应该从数据库统计，简化实现
        log.info("获取质量统计: userId={}, days={}", userId, days);
        
        IEvaluationService.QualityStatistics stats = new IEvaluationService.QualityStatistics();
        stats.setTotalTasks(100);
        stats.setAverageScore(7.5);
        stats.setExcellentCount(60);
        stats.setGoodCount(30);
        stats.setPoorCount(10);
        
        return stats;
    }
    
    /**
     * 评估几何质量
     */
    private double evaluateGeometry(String modelUrl) {
        // 模拟几何质量评估
        return 8.5;
    }
    
    /**
     * 评估纹理质量
     */
    private double evaluateTexture(String modelUrl) {
        // 模拟纹理质量评估
        return 7.8;
    }
    
    /**
     * 评估功能性
     */
    private double evaluateFunctionality(String modelUrl) {
        // 模拟功能性评估
        return 8.0;
    }
    
    /**
     * 分析模型统计信息
     */
    private ModelStats analyzeModelStats(String modelUrl) {
        // 模拟模型统计分析
        return new ModelStats(5000, 2500, 1024000L);
    }
    
    /**
     * 模型统计信息类
     */
    private static class ModelStats {
        private final int faceCount;
        private final int vertexCount;
        private final long fileSize;
        
        public ModelStats(int faceCount, int vertexCount, long fileSize) {
            this.faceCount = faceCount;
            this.vertexCount = vertexCount;
            this.fileSize = fileSize;
        }
        
        public int getFaceCount() { return faceCount; }
        public int getVertexCount() { return vertexCount; }
        public long getFileSize() { return fileSize; }
    }
}