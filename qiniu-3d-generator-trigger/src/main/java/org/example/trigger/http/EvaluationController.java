package org.example.trigger.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.evaluation.model.entity.QualityAssessmentEntity;
import org.example.domain.evaluation.service.IEvaluationService;
import org.example.service.EvaluationApplicationService;
import org.example.types.common.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 效果评估控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@Tag(name = "效果评估", description = "效果评估相关接口")
public class EvaluationController {
    
    @Resource
    private EvaluationApplicationService evaluationApplicationService;
    
    /**
     * 评估3D模型质量
     */
    @PostMapping("/evaluate/{taskId}")
    @Operation(summary = "评估3D模型质量", description = "对指定任务的3D模型进行质量评估")
    public Response<QualityAssessmentEntity> evaluateModel(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        
        return evaluationApplicationService.evaluateModel(taskId);
    }
    
    /**
     * 获取评估报告
     */
    @GetMapping("/report/{taskId}")
    @Operation(summary = "获取评估报告", description = "获取指定任务的评估报告")
    public Response<QualityAssessmentEntity> getAssessmentReport(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        
        return evaluationApplicationService.getAssessmentReport(taskId);
    }
    
    /**
     * 获取质量统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取质量统计", description = "获取用户的质量统计信息")
    public Response<IEvaluationService.QualityStatistics> getQualityStatistics(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") int days) {
        
        return evaluationApplicationService.getQualityStatistics(userId, days);
    }
    
    /**
     * 用户评分
     */
    @PostMapping("/rate/{taskId}")
    @Operation(summary = "用户评分", description = "用户对生成的3D模型进行评分")
    public Response<Void> rateModel(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "评分(1-10)") @RequestParam int rating,
            @Parameter(description = "反馈") @RequestParam(required = false) String feedback) {
        
        return evaluationApplicationService.rateModel(taskId, rating, feedback);
    }
    
    /**
     * 批量评估
     */
    @PostMapping("/batch-evaluate")
    @Operation(summary = "批量评估", description = "批量评估用户的未评估任务")
    public Response<String> batchEvaluate(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "任务数量限制") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // 这里可以实现批量评估逻辑
            log.info("开始批量评估: userId={}, limit={}", userId, limit);
            
            return Response.success("批量评估已启动");
            
        } catch (Exception e) {
            log.error("批量评估失败", e);
            return Response.fail("批量评估失败: " + e.getMessage());
        }
    }
}