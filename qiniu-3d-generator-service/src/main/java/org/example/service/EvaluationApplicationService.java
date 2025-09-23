package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.evaluation.model.entity.QualityAssessmentEntity;
import org.example.domain.evaluation.service.IEvaluationService;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.service.IGenerationService;
import org.example.types.common.Response;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * 评估应用服务
 */
@Slf4j
@Service
public class EvaluationApplicationService {
    
    @Resource
    private IEvaluationService evaluationService;
    
    @Resource
    private IGenerationService generationService;
    
    /**
     * 评估3D模型质量
     */
    public Response<QualityAssessmentEntity> evaluateModel(String taskId) {
        try {
            // 获取生成任务
            GenerationTaskEntity task = generationService.queryTask(taskId);
            if (task == null) {
                return Response.fail("任务不存在");
            }
            
            if (task.getResultUrl() == null) {
                return Response.fail("模型尚未生成完成");
            }
            
            // 执行质量评估
            QualityAssessmentEntity assessment = evaluationService.assessQuality(task);
            
            // 更新任务的质量评分
            task.setQualityScore(assessment.getOverallScore());
            generationService.save(task);
            
            return Response.success(assessment);
            
        } catch (Exception e) {
            log.error("评估模型质量失败: taskId={}", taskId, e);
            return Response.fail("评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步评估模型质量
     */
    public void evaluateModelAsync(String taskId) {
        CompletableFuture.runAsync(() -> {
            try {
                evaluateModel(taskId);
                log.info("异步评估完成: taskId={}", taskId);
            } catch (Exception e) {
                log.error("异步评估失败: taskId={}", taskId, e);
            }
        });
    }
    
    /**
     * 获取评估报告
     */
    public Response<QualityAssessmentEntity> getAssessmentReport(String taskId) {
        try {
            QualityAssessmentEntity assessment = evaluationService.getAssessment(taskId);
            if (assessment == null) {
                return Response.fail("评估报告不存在");
            }
            
            return Response.success(assessment);
            
        } catch (Exception e) {
            log.error("获取评估报告失败: taskId={}", taskId, e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取质量统计
     */
    public Response<IEvaluationService.QualityStatistics> getQualityStatistics(String userId, int days) {
        try {
            IEvaluationService.QualityStatistics statistics = 
                evaluationService.getQualityStatistics(userId, days);
            
            return Response.success(statistics);
            
        } catch (Exception e) {
            log.error("获取质量统计失败: userId={}, days={}", userId, days, e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户评分
     */
    public Response<Void> rateModel(String taskId, int rating, String feedback) {
        try {
            if (rating < 1 || rating > 10) {
                return Response.fail("评分必须在1-10之间");
            }
            
            // 获取任务并更新用户评分
            GenerationTaskEntity task = generationService.queryTask(taskId);
            if (task == null) {
                return Response.fail("任务不存在");
            }
            
            task.setUserRating(rating);
            generationService.update(task);
            
            log.info("用户评分成功: taskId={}, rating={}", taskId, rating);
            
            return Response.success(null);
            
        } catch (Exception e) {
            log.error("用户评分失败: taskId={}, rating={}", taskId, rating, e);
            return Response.fail("评分失败: " + e.getMessage());
        }
    }
}