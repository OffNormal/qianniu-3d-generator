package com.qiniu.model3d.controller;

import com.qiniu.model3d.dto.EvaluationMetrics;
import com.qiniu.model3d.entity.SystemMetrics;
import com.qiniu.model3d.entity.TaskEvaluation;
import com.qiniu.model3d.dto.ApiResponse;
import com.qiniu.model3d.scheduler.MetricsScheduler;
import com.qiniu.model3d.service.EvaluationService;
import com.qiniu.model3d.repository.SystemMetricsRepository;
import com.qiniu.model3d.repository.TaskEvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台仪表板控制器
 * 提供系统监控、统计分析和管理功能
 */
@RestController
@RequestMapping("/admin/dashboard")
@CrossOrigin(origins = "*")
public class AdminDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    
    @Autowired
    private EvaluationService evaluationService;
    
    @Autowired
    private SystemMetricsRepository systemMetricsRepository;
    
    @Autowired
    private TaskEvaluationRepository taskEvaluationRepository;
    
    @Autowired
    private MetricsScheduler metricsScheduler;
    
    /**
     * 获取仪表板概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 获取今日实时数据
            LocalDate today = LocalDate.now();
            EvaluationMetrics todayMetrics = evaluationService.getMetrics(today, today);
            overview.put("today", todayMetrics);
            
            // 获取本周数据
            LocalDate weekStart = today.minusDays(6);
            EvaluationMetrics weekMetrics = evaluationService.getMetrics(weekStart, today);
            overview.put("thisWeek", weekMetrics);
            
            // 获取本月数据
            LocalDate monthStart = today.withDayOfMonth(1);
            EvaluationMetrics monthMetrics = evaluationService.getMetrics(monthStart, today);
            overview.put("thisMonth", monthMetrics);
            
            // 获取最近7天的趋势数据
            List<SystemMetrics> recentTrends = systemMetricsRepository.findRecentMetrics(PageRequest.of(0, 7));
            overview.put("recentTrends", recentTrends);
            
            // 获取系统健康状态
            Map<String, Object> healthStatus = getSystemHealthStatus();
            overview.put("healthStatus", healthStatus);
            
            return ResponseEntity.ok(ApiResponse.success(overview));
        } catch (Exception e) {
            logger.error("获取仪表板概览数据失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取概览数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取详细统计数据
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<EvaluationMetrics>> getDetailedMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            EvaluationMetrics metrics = evaluationService.getMetrics(start, end);
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            logger.error("获取详细统计数据失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取统计数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务列表（分页）
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Page<TaskEvaluation>>> getTaskList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<TaskEvaluation> tasks;
            
            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
                
                if (status != null && !status.isEmpty()) {
                    tasks = taskEvaluationRepository.findByStatusAndCreatedAtBetween(status, start, end, pageable);
                } else {
                    tasks = taskEvaluationRepository.findByCreatedAtBetween(start, end, pageable);
                }
            } else if (status != null && !status.isEmpty()) {
                tasks = taskEvaluationRepository.findByStatus(status, pageable);
            } else {
                tasks = taskEvaluationRepository.findAll(pageable);
            }
            
            return ResponseEntity.ok(ApiResponse.success(tasks));
        } catch (Exception e) {
            logger.error("获取任务列表失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取任务列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统指标历史数据
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<ApiResponse<List<SystemMetrics>>> getMetricsHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            List<SystemMetrics> history;
            
            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                history = systemMetricsRepository.findByMetricDateBetween(start, end);
            } else {
                history = systemMetricsRepository.findRecentMetrics(PageRequest.of(0, days));
            }
            
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            logger.error("获取系统指标历史数据失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取历史数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手动触发统计任务
     */
    @PostMapping("/metrics/generate")
    public ResponseEntity<ApiResponse<String>> generateMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                metricsScheduler.manualGenerateMetrics(start, end);
                return ResponseEntity.ok(ApiResponse.success("手动统计任务已完成", "手动统计任务已完成"));
            } else {
                // 生成今天的统计数据
                metricsScheduler.generateMetricsForDate(LocalDate.now());
                return ResponseEntity.ok(ApiResponse.success("今日统计数据已生成", "今日统计数据已生成"));
            }
        } catch (Exception e) {
            logger.error("手动生成统计数据失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("生成统计数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            Map<String, Object> healthStatus = getSystemHealthStatus();
            return ResponseEntity.ok(ApiResponse.success(healthStatus));
        } catch (Exception e) {
            logger.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取健康状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取实时统计数据
     */
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtimeStats() {
        try {
            Map<String, Object> realtimeStats = new HashMap<>();
            
            // 今日实时数据
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime now = LocalDateTime.now();
            
            Long todayTasks = taskEvaluationRepository.countTasksByDateRange(startOfDay, now);
            Long todaySuccess = taskEvaluationRepository.countTasksByStatusAndDateRange("DONE", startOfDay, now);
            Long todayFailed = taskEvaluationRepository.countTasksByStatusAndDateRange("FAIL", startOfDay, now);
            Long todayPending = taskEvaluationRepository.countTasksByStatusAndDateRange("SUBMITTED", startOfDay, now);
            
            realtimeStats.put("todayTasks", todayTasks);
            realtimeStats.put("todaySuccess", todaySuccess);
            realtimeStats.put("todayFailed", todayFailed);
            realtimeStats.put("todayPending", todayPending);
            realtimeStats.put("todaySuccessRate", todayTasks > 0 ? (double) todaySuccess / todayTasks * 100 : 0.0);
            
            // 最近1小时数据
            LocalDateTime oneHourAgo = now.minusHours(1);
            Long hourlyTasks = taskEvaluationRepository.countTasksByDateRange(oneHourAgo, now);
            realtimeStats.put("hourlyTasks", hourlyTasks);
            
            return ResponseEntity.ok(ApiResponse.success(realtimeStats));
        } catch (Exception e) {
            logger.error("获取实时统计数据失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取实时数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统健康状态的私有方法
     */
    private Map<String, Object> getSystemHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            // 检查最近24小时的成功率
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime now = LocalDateTime.now();
            
            Long totalTasks = taskEvaluationRepository.countTasksByDateRange(yesterday, now);
            Long successTasks = taskEvaluationRepository.countTasksByStatusAndDateRange("DONE", yesterday, now);
            
            double successRate = totalTasks > 0 ? (double) successTasks / totalTasks * 100 : 0.0;
            
            // 健康状态评估
            String status;
            if (successRate >= 90) {
                status = "excellent";
            } else if (successRate >= 80) {
                status = "good";
            } else if (successRate >= 70) {
                status = "warning";
            } else {
                status = "critical";
            }
            
            healthStatus.put("status", status);
            healthStatus.put("successRate", successRate);
            healthStatus.put("totalTasks24h", totalTasks);
            healthStatus.put("successTasks24h", successTasks);
            
            // 检查平均响应时间
            Double avgDuration = taskEvaluationRepository.getAverageDurationByDateRange(yesterday, now);
            healthStatus.put("avgResponseTime", avgDuration != null ? avgDuration : 0.0);
            
            // 检查错误率
            Long failedTasks = taskEvaluationRepository.countTasksByStatusAndDateRange("FAIL", yesterday, now);
            double errorRate = totalTasks > 0 ? (double) failedTasks / totalTasks * 100 : 0.0;
            healthStatus.put("errorRate", errorRate);
            
        } catch (Exception e) {
            logger.error("计算系统健康状态失败", e);
            healthStatus.put("status", "unknown");
            healthStatus.put("error", e.getMessage());
        }
        
        return healthStatus;
    }
}