package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存预热服务接口
 * 负责智能预加载热门任务和预测性缓存
 */
public interface CacheWarmupService {

    /**
     * 执行缓存预热
     * 
     * @return 预热的任务数量
     */
    int performWarmup();

    /**
     * 基于历史数据预热热门任务
     * 
     * @param days 分析的历史天数
     * @param limit 预热任务数量限制
     * @return 预热的任务数量
     */
    int warmupPopularTasks(int days, int limit);

    /**
     * 基于时间模式预热任务
     * 
     * @param hour 当前小时
     * @param limit 预热任务数量限制
     * @return 预热的任务数量
     */
    int warmupByTimePattern(int hour, int limit);

    /**
     * 基于用户行为预热任务
     * 
     * @param clientIp 客户端IP
     * @param limit 预热任务数量限制
     * @return 预热的任务数量
     */
    int warmupByUserBehavior(String clientIp, int limit);

    /**
     * 预热相似任务
     * 
     * @param recentTasks 最近的任务列表
     * @param limit 预热任务数量限制
     * @return 预热的任务数量
     */
    int warmupSimilarTasks(List<ModelTask> recentTasks, int limit);

    /**
     * 获取预热候选任务
     * 
     * @param strategy 预热策略
     * @param limit 候选数量限制
     * @return 候选任务列表
     */
    List<ModelTask> getWarmupCandidates(WarmupStrategy strategy, int limit);

    /**
     * 检查是否需要预热
     * 
     * @return 是否需要预热
     */
    boolean shouldPerformWarmup();

    /**
     * 获取预热统计信息
     * 
     * @return 预热统计信息
     */
    WarmupStatistics getWarmupStatistics();

    /**
     * 预热策略枚举
     */
    enum WarmupStrategy {
        POPULAR_TASKS,      // 热门任务
        TIME_PATTERN,       // 时间模式
        USER_BEHAVIOR,      // 用户行为
        SIMILAR_TASKS,      // 相似任务
        COMPREHENSIVE       // 综合策略
    }

    /**
     * 预热统计信息
     */
    class WarmupStatistics {
        private final int totalWarmedTasks;
        private final int popularTasksWarmed;
        private final int timePatternWarmed;
        private final int userBehaviorWarmed;
        private final int similarTasksWarmed;
        private final LocalDateTime lastWarmupTime;
        private final long warmupDurationMs;

        public WarmupStatistics(int totalWarmedTasks, int popularTasksWarmed, 
                              int timePatternWarmed, int userBehaviorWarmed, 
                              int similarTasksWarmed, LocalDateTime lastWarmupTime, 
                              long warmupDurationMs) {
            this.totalWarmedTasks = totalWarmedTasks;
            this.popularTasksWarmed = popularTasksWarmed;
            this.timePatternWarmed = timePatternWarmed;
            this.userBehaviorWarmed = userBehaviorWarmed;
            this.similarTasksWarmed = similarTasksWarmed;
            this.lastWarmupTime = lastWarmupTime;
            this.warmupDurationMs = warmupDurationMs;
        }

        // Getters
        public int getTotalWarmedTasks() { return totalWarmedTasks; }
        public int getPopularTasksWarmed() { return popularTasksWarmed; }
        public int getTimePatternWarmed() { return timePatternWarmed; }
        public int getUserBehaviorWarmed() { return userBehaviorWarmed; }
        public int getSimilarTasksWarmed() { return similarTasksWarmed; }
        public LocalDateTime getLastWarmupTime() { return lastWarmupTime; }
        public long getWarmupDurationMs() { return warmupDurationMs; }
    }
}