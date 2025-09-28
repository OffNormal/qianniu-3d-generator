package com.qiniu.model3d.service;

import com.qiniu.model3d.dto.EvaluationMetrics;
import com.qiniu.model3d.dto.QueryHunyuanTo3DJobResponse;
import com.qiniu.model3d.dto.TaskEvaluationData;
import com.qiniu.model3d.entity.SystemMetrics;
import com.qiniu.model3d.entity.TaskEvaluation;
import com.qiniu.model3d.repository.SystemMetricsRepository;
import com.qiniu.model3d.repository.TaskEvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评估服务核心类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

    @Autowired
    private TaskEvaluationRepository taskEvaluationRepository;

    @Autowired
    private SystemMetricsRepository systemMetricsRepository;

    /**
     * 记录任务提交信息
     * 
     * @param jobId 任务ID
     * @param prompt 提示词
     * @param resultFormat 结果格式
     * @param submitTime 提交时间
     * @param clientIp 客户端IP
     */
    @Transactional
    public void recordTaskSubmission(String jobId, String prompt, String resultFormat, LocalDateTime submitTime, String clientIp) {
        try {
            TaskEvaluation evaluation = new TaskEvaluation();
            evaluation.setJobId(jobId);
            evaluation.setPrompt(prompt);
            evaluation.setResultFormat(resultFormat);
            evaluation.setStatus("SUBMITTED");
            evaluation.setSubmitTime(submitTime);
            evaluation.setClientIp(clientIp);
            
            taskEvaluationRepository.save(evaluation);
            logger.info("记录任务提交信息成功: jobId={}", jobId);
        } catch (Exception e) {
            logger.error("记录任务提交信息失败: jobId={}", jobId, e);
        }
    }

    /**
     * 记录任务评估数据
     */
    @Transactional
    public void recordTaskEvaluation(String jobId, TaskEvaluationData data) {
        try {
            TaskEvaluation evaluation = taskEvaluationRepository.findByJobId(jobId)
                    .orElse(new TaskEvaluation());

            // 设置基本信息
            evaluation.setJobId(jobId);
            evaluation.setPrompt(data.getPrompt());
            evaluation.setResultFormat(data.getResultFormat());
            evaluation.setStatus(data.getStatus());
            evaluation.setClientIp(data.getClientIp());
            evaluation.setErrorMessage(data.getErrorMessage());

            // 设置时间信息
            if (data.getSubmitTime() != null) {
                evaluation.setSubmitTime(data.getSubmitTime());
            }
            if (data.getCompleteTime() != null) {
                evaluation.setCompleteTime(data.getCompleteTime());
            }
            if (data.getDurationSeconds() != null) {
                evaluation.setDurationSeconds(data.getDurationSeconds());
            }
            if (data.getFileSizeKb() != null) {
                evaluation.setFileSizeKb(data.getFileSizeKb());
            }

            taskEvaluationRepository.save(evaluation);
            logger.info("Task evaluation recorded for jobId: {}", jobId);

        } catch (Exception e) {
            logger.error("Failed to record task evaluation for jobId: {}", jobId, e);
        }
    }

    /**
     * 更新任务状态和完成信息
     * 
     * @param jobId 任务ID
     * @param status 任务状态
     * @param completeTime 完成时间
     * @param errorMessage 错误信息
     */
    @Transactional
    public void updateTaskStatus(String jobId, String status, LocalDateTime completeTime, String errorMessage) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                
                // 更新任务状态
                if (status != null) {
                    evaluation.setStatus(status);
                }
                
                // 如果任务完成，记录完成时间和计算耗时
                if (("SUCCESS".equals(status) || "DONE".equals(status)) && completeTime != null) {
                    evaluation.setCompleteTime(completeTime);
                    
                    // 计算耗时（秒）
                    if (evaluation.getSubmitTime() != null) {
                        Duration duration = Duration.between(evaluation.getSubmitTime(), completeTime);
                        evaluation.setDurationSeconds((int) duration.getSeconds());
                    }
                }
                
                // 如果任务失败，记录错误信息
                if (("FAIL".equals(status) || "FAILED".equals(status)) && errorMessage != null) {
                    evaluation.setErrorMessage(errorMessage);
                    if (completeTime != null) {
                        evaluation.setCompleteTime(completeTime);
                    }
                }
                
                evaluation.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(evaluation);
                logger.info("更新任务状态成功: jobId={}, status={}", jobId, status);
            } else {
                logger.warn("未找到任务评估记录: jobId={}", jobId);
            }
        } catch (Exception e) {
            logger.error("更新任务状态失败: jobId={}", jobId, e);
        }
    }

    /**
     * 更新任务状态和完成信息
     * 
     * @param jobId 任务ID
     * @param response 查询响应
     */
    @Transactional
    public void updateTaskStatus(String jobId, QueryHunyuanTo3DJobResponse response) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                
                // 更新任务状态
                if (response.getStatus() != null) {
                    evaluation.setStatus(response.getStatus());
                }
                
                // 如果任务完成，记录完成时间和计算耗时
                if ("DONE".equals(response.getStatus()) && evaluation.getCompleteTime() == null) {
                    LocalDateTime completeTime = LocalDateTime.now();
                    evaluation.setCompleteTime(completeTime);
                    
                    // 计算耗时（秒）
                    if (evaluation.getSubmitTime() != null) {
                        Duration duration = Duration.between(evaluation.getSubmitTime(), completeTime);
                        evaluation.setDurationSeconds((int) duration.getSeconds());
                    }
                    
                    // 记录文件大小（如果有文件信息）
                    if (response.getResultFile3Ds() != null && !response.getResultFile3Ds().isEmpty()) {
                        // 这里可以根据实际需要获取文件大小信息
                        // evaluation.setFileSizeKb(fileSizeKb);
                    }
                }
                
                // 如果任务失败，记录错误信息
                if ("FAIL".equals(response.getStatus()) && response.getErrorMessage() != null) {
                    evaluation.setErrorMessage(response.getErrorMessage());
                    evaluation.setCompleteTime(LocalDateTime.now());
                }
                
                evaluation.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(evaluation);
                logger.info("更新任务状态成功: jobId={}, status={}", jobId, response.getStatus());
            } else {
                logger.warn("未找到任务评估记录: jobId={}", jobId);
            }
        } catch (Exception e) {
            logger.error("更新任务状态失败: jobId={}", jobId, e);
        }
    }

    /**
     * 记录下载行为
     * 
     * @param jobId 任务ID
     * @param fileSizeBytes 文件大小（字节）
     */
    @Transactional
    public void recordDownload(String jobId, int fileSizeBytes) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                evaluation.incrementDownloadCount();
                
                // 更新文件大小（转换为KB）
                if (evaluation.getFileSizeKb() == null) {
                    evaluation.setFileSizeKb(fileSizeBytes / 1024);
                }
                
                evaluation.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(evaluation);
                logger.info("记录下载行为成功: jobId={}, fileSize={}KB", jobId, fileSizeBytes / 1024);
            } else {
                logger.warn("未找到任务评估记录: jobId={}", jobId);
            }
        } catch (Exception e) {
            logger.error("记录下载行为失败: jobId={}", jobId, e);
        }
    }

    /**
     * 记录预览行为
     * 
     * @param jobId 任务ID
     */
    @Transactional
    public void recordPreview(String jobId) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                evaluation.incrementPreviewCount();
                evaluation.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(evaluation);
                logger.info("记录预览行为成功: jobId={}", jobId);
            } else {
                logger.warn("未找到任务评估记录: jobId={}", jobId);
            }
        } catch (Exception e) {
            logger.error("记录预览行为失败: jobId={}", jobId, e);
        }
    }

    /**
     * 更新用户反馈评分
     * 
     * @param jobId 任务ID
     * @param rating 用户评分
     */
    @Transactional
    public void updateUserFeedback(String jobId, int rating) {
        try {
            Optional<TaskEvaluation> optionalTask = taskEvaluationRepository.findByJobId(jobId);
            if (optionalTask.isPresent()) {
                TaskEvaluation task = optionalTask.get();
                task.setUserRating(rating);
                task.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(task);
                logger.info("更新用户反馈: jobId={}, rating={}", jobId, rating);
            } else {
                logger.warn("未找到任务记录，无法更新用户反馈: jobId={}", jobId);
                throw new RuntimeException("任务记录不存在: " + jobId);
            }
        } catch (Exception e) {
            logger.error("更新用户反馈失败: jobId={}, rating={}", jobId, rating, e);
            throw e;
        }
    }

    /**
     * 更新用户反馈（评分、下载、预览计数）
     * 
     * @param jobId 任务ID
     * @param rating 用户评分（1-5星）
     */
    @Transactional
    public void updateUserFeedback(String jobId, Integer rating) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                if (rating != null && rating >= 1 && rating <= 5) {
                    evaluation.setUserRating(rating);
                }
                evaluation.setUpdatedAt(LocalDateTime.now());
                taskEvaluationRepository.save(evaluation);
                logger.info("更新用户反馈成功: jobId={}, rating={}", jobId, rating);
            } else {
                logger.warn("未找到任务评估记录: jobId={}", jobId);
            }
        } catch (Exception e) {
            logger.error("更新用户反馈失败: jobId={}", jobId, e);
        }
    }

    /**
     * 增加下载计数
     */
    @Transactional
    public void incrementDownloadCount(String jobId) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                evaluation.incrementDownloadCount();
                taskEvaluationRepository.save(evaluation);
                logger.debug("Download count incremented for jobId: {}", jobId);
            }
        } catch (Exception e) {
            logger.error("Failed to increment download count for jobId: {}", jobId, e);
        }
    }

    /**
     * 增加预览计数
     */
    @Transactional
    public void incrementPreviewCount(String jobId) {
        try {
            Optional<TaskEvaluation> optionalEvaluation = taskEvaluationRepository.findByJobId(jobId);
            if (optionalEvaluation.isPresent()) {
                TaskEvaluation evaluation = optionalEvaluation.get();
                evaluation.incrementPreviewCount();
                taskEvaluationRepository.save(evaluation);
                logger.debug("Preview count incremented for jobId: {}", jobId);
            }
        } catch (Exception e) {
            logger.error("Failed to increment preview count for jobId: {}", jobId, e);
        }
    }

    /**
     * 获取指定日期范围的评估指标
     */
    public EvaluationMetrics getMetrics(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

            EvaluationMetrics metrics = new EvaluationMetrics(startDate, endDate);

            // 基础统计数据
            Long totalTasks = taskEvaluationRepository.countByCreatedAtBetween(startTime, endTime);
            Long successTasks = taskEvaluationRepository.countSuccessTasksByCreatedAtBetween(startTime, endTime);
            Long failedTasks = taskEvaluationRepository.countFailedTasksByCreatedAtBetween(startTime, endTime);

            metrics.setTotalTasks(totalTasks.intValue());
            metrics.setSuccessTasks(successTasks.intValue());
            metrics.setFailedTasks(failedTasks.intValue());

            // 计算成功率
            if (totalTasks > 0) {
                BigDecimal successRate = BigDecimal.valueOf(successTasks)
                        .divide(BigDecimal.valueOf(totalTasks), 4, BigDecimal.ROUND_HALF_UP);
                metrics.setSuccessRate(successRate);
            }

            // 平均执行时间
            Double avgDuration = taskEvaluationRepository.getAvgDurationByCreatedAtBetween(startTime, endTime);
            if (avgDuration != null) {
                metrics.setAvgDurationSeconds(BigDecimal.valueOf(avgDuration));
            }

            // 用户相关指标
            Long totalUsers = taskEvaluationRepository.countDistinctUsersByCreatedAtBetween(startTime, endTime);
            metrics.setTotalUsers(totalUsers.intValue());

            Double avgRating = taskEvaluationRepository.getAvgRatingByCreatedAtBetween(startTime, endTime);
            if (avgRating != null) {
                metrics.setAvgRating(BigDecimal.valueOf(avgRating));
            }

            // 下载和预览统计
            Long totalDownloads = taskEvaluationRepository.getTotalDownloadsByCreatedAtBetween(startTime, endTime);
            Long totalPreviews = taskEvaluationRepository.getTotalPreviewsByCreatedAtBetween(startTime, endTime);
            
            metrics.setTotalDownloads(totalDownloads != null ? totalDownloads.intValue() : 0);
            metrics.setTotalPreviews(totalPreviews != null ? totalPreviews.intValue() : 0);

            // 计算下载率
            if (successTasks > 0 && totalDownloads != null) {
                BigDecimal downloadRate = BigDecimal.valueOf(totalDownloads)
                        .divide(BigDecimal.valueOf(successTasks), 4, BigDecimal.ROUND_HALF_UP);
                metrics.setDownloadRate(downloadRate);
            }

            // 获取分布数据
            metrics.setFormatDistribution(getFormatDistribution(startTime, endTime));
            metrics.setRatingDistribution(getRatingDistribution(startTime, endTime));
            metrics.setPopularPrompts(getPopularPrompts(startTime, endTime, 10));

            // 获取日度趋势数据
            metrics.setDailyTrends(getDailyTrends(startDate, endDate));

            logger.info("Evaluation metrics generated for period: {} to {}", startDate, endDate);
            return metrics;

        } catch (Exception e) {
            logger.error("Failed to get evaluation metrics for period: {} to {}", startDate, endDate, e);
            return new EvaluationMetrics(startDate, endDate);
        }
    }

    /**
     * 获取格式分布统计
     */
    private Map<String, Integer> getFormatDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = taskEvaluationRepository.getFormatDistributionByCreatedAtBetween(startTime, endTime);
        return results.stream()
                .filter(row -> row[0] != null) // 过滤掉null键
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 获取评分分布统计
     */
    private Map<Integer, Integer> getRatingDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = taskEvaluationRepository.getRatingDistributionByCreatedAtBetween(startTime, endTime);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> ((Long) row[1]).intValue(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 获取热门提示词
     */
    private List<EvaluationMetrics.PopularPrompt> getPopularPrompts(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<Object[]> results = taskEvaluationRepository.getPopularPromptsWithSuccessRate(startTime, endTime);
        return results.stream()
                .limit(limit)
                .map(row -> {
                    String text = (String) row[0];
                    Integer count = ((Number) row[1]).intValue();
                    BigDecimal successRate = (BigDecimal) row[2];
                    return new EvaluationMetrics.PopularPrompt(text, count, successRate);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取日度趋势数据
     */
    private List<EvaluationMetrics.DailyMetrics> getDailyTrends(LocalDate startDate, LocalDate endDate) {
        List<SystemMetrics> systemMetrics = systemMetricsRepository.findByMetricDateBetween(startDate, endDate);
        
        return systemMetrics.stream()
                .map(sm -> {
                    EvaluationMetrics.DailyMetrics daily = new EvaluationMetrics.DailyMetrics();
                    daily.setDate(sm.getMetricDate());
                    daily.setTasks(sm.getTotalTasks());
                    daily.setSuccessRate(sm.getSuccessRate());
                    daily.setAvgRating(sm.getAvgRating());
                    daily.setDownloads(sm.getTotalDownloads());
                    return daily;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取实时统计数据（今日数据）
     */
    public EvaluationMetrics getTodayMetrics() {
        LocalDate today = LocalDate.now();
        return getMetrics(today, today);
    }

    /**
     * 获取最近N天的统计数据
     */
    public EvaluationMetrics getRecentMetrics(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        return getMetrics(startDate, endDate);
    }

    /**
     * 检查是否需要告警
     */
    public List<String> checkAlerts() {
        List<String> alerts = new ArrayList<>();
        
        try {
            // 检查今日成功率
            EvaluationMetrics todayMetrics = getTodayMetrics();
            if (todayMetrics.getSuccessRate() != null && 
                todayMetrics.getSuccessRate().compareTo(BigDecimal.valueOf(0.8)) < 0 &&
                todayMetrics.getTotalTasks() > 10) {
                alerts.add("今日成功率低于80%: " + todayMetrics.getSuccessRate().multiply(BigDecimal.valueOf(100)) + "%");
            }

            // 检查平均评分
            if (todayMetrics.getAvgRating() != null && 
                todayMetrics.getAvgRating().compareTo(BigDecimal.valueOf(3.0)) < 0) {
                alerts.add("今日平均评分低于3.0: " + todayMetrics.getAvgRating());
            }

            // 检查平均响应时间
            if (todayMetrics.getAvgDurationSeconds() != null && 
                todayMetrics.getAvgDurationSeconds().compareTo(BigDecimal.valueOf(300)) > 0) {
                alerts.add("今日平均响应时间超过5分钟: " + todayMetrics.getAvgDurationSeconds() + "秒");
            }

        } catch (Exception e) {
            logger.error("Failed to check alerts", e);
            alerts.add("告警检查失败: " + e.getMessage());
        }

        return alerts;
    }
}