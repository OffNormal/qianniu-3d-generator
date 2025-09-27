package com.qiniu.model3d.scheduler;

import com.qiniu.model3d.entity.SystemMetrics;
import com.qiniu.model3d.repository.SystemMetricsRepository;
import com.qiniu.model3d.repository.TaskEvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统指标定时统计任务
 * 每天凌晨1点执行，生成前一天的统计数据
 */
@Component
public class MetricsScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsScheduler.class);
    
    @Autowired
    private TaskEvaluationRepository taskEvaluationRepository;
    
    @Autowired
    private SystemMetricsRepository systemMetricsRepository;
    
    /**
     * 每天凌晨1点执行日度统计
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void generateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        generateMetricsForDate(yesterday);
    }
    
    /**
     * 每小时执行当天实时统计（用于实时监控）
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void generateHourlyMetrics() {
        LocalDate today = LocalDate.now();
        generateMetricsForDate(today);
    }
    
    /**
     * 为指定日期生成统计数据
     */
    public void generateMetricsForDate(LocalDate date) {
        try {
            logger.info("开始生成日期 {} 的统计数据", date);
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            // 检查是否已存在该日期的数据
            boolean exists = systemMetricsRepository.existsByMetricDate(date);
            
            // 统计基础数据
            Long totalTasks = taskEvaluationRepository.countTasksByDateRange(startOfDay, endOfDay);
            Long successTasks = taskEvaluationRepository.countTasksByStatusAndDateRange("DONE", startOfDay, endOfDay);
            Long failedTasks = taskEvaluationRepository.countTasksByStatusAndDateRange("FAIL", startOfDay, endOfDay);
            
            // 计算平均时长
            Double avgDurationSeconds = taskEvaluationRepository.getAverageDurationByDateRange(startOfDay, endOfDay);
            if (avgDurationSeconds == null) {
                avgDurationSeconds = 0.0;
            }
            
            // 统计用户数
            Long totalUsers = taskEvaluationRepository.countDistinctUsersByDateRange(startOfDay, endOfDay);
            
            // 计算平均评分
            Double avgRating = taskEvaluationRepository.getAverageRatingByDateRange(startOfDay, endOfDay);
            if (avgRating == null) {
                avgRating = 0.0;
            }
            
            // 统计下载和预览数
            Long totalDownloads = taskEvaluationRepository.getTotalDownloadsByDateRange(startOfDay, endOfDay);
            Long totalPreviews = taskEvaluationRepository.getTotalPreviewsByDateRange(startOfDay, endOfDay);
            
            if (totalDownloads == null) totalDownloads = 0L;
            if (totalPreviews == null) totalPreviews = 0L;
            
            // 计算成功率
            double successRate = totalTasks > 0 ? (double) successTasks / totalTasks * 100 : 0.0;
            
            // 创建或更新系统指标记录
            SystemMetrics metrics;
            if (exists) {
                metrics = systemMetricsRepository.findByMetricDate(date).orElse(new SystemMetrics());
            } else {
                metrics = new SystemMetrics();
                metrics.setMetricDate(date);
                metrics.setCreatedAt(LocalDateTime.now());
            }
            
            metrics.setTotalTasks(totalTasks.intValue());
            metrics.setSuccessTasks(successTasks.intValue());
            metrics.setFailedTasks(failedTasks.intValue());
            metrics.setAvgDurationSeconds(BigDecimal.valueOf(avgDurationSeconds).setScale(2, RoundingMode.HALF_UP));
            metrics.setTotalUsers(totalUsers.intValue());
            metrics.setAvgRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
            metrics.setTotalDownloads(totalDownloads.intValue());
            metrics.setTotalPreviews(totalPreviews.intValue());
            metrics.calculateSuccessRate(); // 自动计算成功率
            
            systemMetricsRepository.save(metrics);
            
            logger.info("完成日期 {} 的统计数据生成: 总任务={}, 成功={}, 失败={}, 成功率={}%, 平均时长={}s, 用户数={}, 平均评分={}", 
                    date, totalTasks, successTasks, failedTasks, 
                    String.format("%.2f", successRate), 
                    String.format("%.2f", avgDurationSeconds), 
                    totalUsers, String.format("%.2f", avgRating));
                    
        } catch (Exception e) {
            logger.error("生成日期 {} 的统计数据失败", date, e);
        }
    }
    
    /**
     * 手动触发统计任务（用于测试或补充数据）
     */
    public void manualGenerateMetrics(LocalDate startDate, LocalDate endDate) {
        logger.info("手动生成统计数据: {} 到 {}", startDate, endDate);
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            generateMetricsForDate(current);
            current = current.plusDays(1);
        }
        
        logger.info("手动统计任务完成");
    }
    
    /**
     * 清理旧数据（保留最近90天的数据）
     */
    @Scheduled(cron = "0 30 2 * * ?") // 每天凌晨2:30执行
    @Transactional
    public void cleanupOldData() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(90);
            int deletedCount = systemMetricsRepository.deleteOldData(cutoffDate);
            logger.info("清理旧统计数据完成，删除了 {} 条记录（早于 {}）", deletedCount, cutoffDate);
        } catch (Exception e) {
            logger.error("清理旧统计数据失败", e);
        }
    }
}