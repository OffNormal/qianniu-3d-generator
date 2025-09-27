package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelTaskRepository;
import com.qiniu.model3d.service.CacheWarmupService;
import com.qiniu.model3d.service.SimilarityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存预热服务实现
 * 实现智能预热策略，包括热门任务、时间模式、用户行为和相似任务预热
 */
@Service
public class CacheWarmupServiceImpl implements CacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupServiceImpl.class);

    @Autowired
    private ModelTaskRepository taskRepository;

    @Autowired
    private SimilarityService similarityService;

    // 预热配置参数
    @Value("${cache.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Value("${cache.warmup.popular-tasks.days:7}")
    private int popularTasksDays;

    @Value("${cache.warmup.popular-tasks.limit:50}")
    private int popularTasksLimit;

    @Value("${cache.warmup.time-pattern.limit:30}")
    private int timePatternLimit;

    @Value("${cache.warmup.user-behavior.limit:20}")
    private int userBehaviorLimit;

    @Value("${cache.warmup.similar-tasks.limit:25}")
    private int similarTasksLimit;

    @Value("${cache.warmup.similarity.threshold:0.7}")
    private double similarityThreshold;

    @Value("${cache.warmup.interval.hours:6}")
    private int warmupIntervalHours;

    // 预热统计
    private WarmupStatistics lastWarmupStats;
    private LocalDateTime lastWarmupTime;

    @Override
    @Transactional
    public int performWarmup() {
        if (!warmupEnabled) {
            logger.debug("Cache warmup is disabled");
            return 0;
        }

        if (!shouldPerformWarmup()) {
            logger.debug("Warmup not needed at this time");
            return 0;
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting comprehensive cache warmup");

        try {
            int totalWarmed = 0;
            int popularWarmed = 0;
            int timePatternWarmed = 0;
            int userBehaviorWarmed = 0;
            int similarWarmed = 0;

            // 1. 预热热门任务
            popularWarmed = warmupPopularTasks(popularTasksDays, popularTasksLimit);
            totalWarmed += popularWarmed;

            // 2. 基于时间模式预热
            int currentHour = LocalTime.now().getHour();
            timePatternWarmed = warmupByTimePattern(currentHour, timePatternLimit);
            totalWarmed += timePatternWarmed;

            // 3. 基于用户行为预热（预热最活跃用户的相关任务）
            userBehaviorWarmed = warmupActiveUserTasks();
            totalWarmed += userBehaviorWarmed;

            // 4. 预热相似任务
            List<ModelTask> recentTasks = getRecentTasks(1, 20);
            similarWarmed = warmupSimilarTasks(recentTasks, similarTasksLimit);
            totalWarmed += similarWarmed;

            long duration = System.currentTimeMillis() - startTime;
            lastWarmupTime = LocalDateTime.now();
            lastWarmupStats = new WarmupStatistics(
                totalWarmed, popularWarmed, timePatternWarmed, 
                userBehaviorWarmed, similarWarmed, lastWarmupTime, duration
            );

            logger.info("Cache warmup completed: {} tasks warmed in {}ms", totalWarmed, duration);
            return totalWarmed;

        } catch (Exception e) {
            logger.error("Error during cache warmup", e);
            return 0;
        }
    }

    @Override
    public int warmupPopularTasks(int days, int limit) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<ModelTask> popularTasks = taskRepository.findHotCacheTasks(
                PageRequest.of(0, limit)).getContent();

            int warmedCount = 0;
            for (ModelTask task : popularTasks) {
                if (isWarmupCandidate(task)) {
                    performTaskWarmup(task);
                    warmedCount++;
                }
            }

            logger.debug("Warmed {} popular tasks from last {} days", warmedCount, days);
            return warmedCount;
        } catch (Exception e) {
            logger.error("Error warming up popular tasks", e);
            return 0;
        }
    }

    @Override
    public int warmupByTimePattern(int hour, int limit) {
        try {
            // 基于小时模式查找历史上这个时间段的热门任务
            List<ModelTask> timePatternTasks = taskRepository.findTasksByHourPattern(
                hour, PageRequest.of(0, limit));

            int warmedCount = 0;
            for (ModelTask task : timePatternTasks) {
                if (isWarmupCandidate(task)) {
                    performTaskWarmup(task);
                    warmedCount++;
                }
            }

            logger.debug("Warmed {} tasks based on time pattern for hour {}", warmedCount, hour);
            return warmedCount;
        } catch (Exception e) {
            logger.error("Error warming up tasks by time pattern", e);
            return 0;
        }
    }

    @Override
    public int warmupByUserBehavior(String clientIp, int limit) {
        try {
            List<ModelTask> userTasks = taskRepository.findRecentTasksByClientIp(
                clientIp, PageRequest.of(0, limit));

            int warmedCount = 0;
            for (ModelTask task : userTasks) {
                if (isWarmupCandidate(task)) {
                    performTaskWarmup(task);
                    warmedCount++;
                }
            }

            logger.debug("Warmed {} tasks for user behavior pattern (IP: {})", warmedCount, clientIp);
            return warmedCount;
        } catch (Exception e) {
            logger.error("Error warming up tasks by user behavior", e);
            return 0;
        }
    }

    @Override
    public int warmupSimilarTasks(List<ModelTask> recentTasks, int limit) {
        try {
            Set<String> warmedTaskIds = new HashSet<>();
            int warmedCount = 0;

            for (ModelTask recentTask : recentTasks) {
                if (warmedCount >= limit) break;

                List<ModelTask> similarTasks = findSimilarTasksForWarmup(recentTask, 5);
                for (ModelTask similarTask : similarTasks) {
                    if (warmedCount >= limit) break;
                    if (warmedTaskIds.contains(similarTask.getTaskId())) continue;

                    if (isWarmupCandidate(similarTask)) {
                        performTaskWarmup(similarTask);
                        warmedTaskIds.add(similarTask.getTaskId());
                        warmedCount++;
                    }
                }
            }

            logger.debug("Warmed {} similar tasks", warmedCount);
            return warmedCount;
        } catch (Exception e) {
            logger.error("Error warming up similar tasks", e);
            return 0;
        }
    }

    @Override
    public List<ModelTask> getWarmupCandidates(WarmupStrategy strategy, int limit) {
        try {
            switch (strategy) {
                case POPULAR_TASKS:
                    return taskRepository.findHotCacheTasks(PageRequest.of(0, limit)).getContent();
                
                case TIME_PATTERN:
                    int currentHour = LocalTime.now().getHour();
                    return taskRepository.findTasksByHourPattern(currentHour, PageRequest.of(0, limit));
                
                case USER_BEHAVIOR:
                    return taskRepository.findMostActiveUserTasks(PageRequest.of(0, limit));
                
                case SIMILAR_TASKS:
                    List<ModelTask> recentTasks = getRecentTasks(1, 10);
                    return recentTasks.stream()
                        .flatMap(task -> findSimilarTasksForWarmup(task, 3).stream())
                        .distinct()
                        .limit(limit)
                        .collect(Collectors.toList());
                
                case COMPREHENSIVE:
                default:
                    List<ModelTask> candidates = new ArrayList<>();
                    candidates.addAll(getWarmupCandidates(WarmupStrategy.POPULAR_TASKS, limit / 4));
                    candidates.addAll(getWarmupCandidates(WarmupStrategy.TIME_PATTERN, limit / 4));
                    candidates.addAll(getWarmupCandidates(WarmupStrategy.USER_BEHAVIOR, limit / 4));
                    candidates.addAll(getWarmupCandidates(WarmupStrategy.SIMILAR_TASKS, limit / 4));
                    return candidates.stream().distinct().limit(limit).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error getting warmup candidates for strategy: {}", strategy, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean shouldPerformWarmup() {
        if (!warmupEnabled) {
            return false;
        }

        if (lastWarmupTime == null) {
            return true;
        }

        LocalDateTime nextWarmupTime = lastWarmupTime.plusHours(warmupIntervalHours);
        return LocalDateTime.now().isAfter(nextWarmupTime);
    }

    @Override
    public WarmupStatistics getWarmupStatistics() {
        if (lastWarmupStats == null) {
            return new WarmupStatistics(0, 0, 0, 0, 0, null, 0);
        }
        return lastWarmupStats;
    }

    /**
     * 预热最活跃用户的任务
     */
    private int warmupActiveUserTasks() {
        try {
            List<String> activeIps = taskRepository.findMostActiveClientIps(PageRequest.of(0, 5));
            int totalWarmed = 0;

            for (String ip : activeIps) {
                int warmed = warmupByUserBehavior(ip, userBehaviorLimit / activeIps.size());
                totalWarmed += warmed;
            }

            return totalWarmed;
        } catch (Exception e) {
            logger.error("Error warming up active user tasks", e);
            return 0;
        }
    }

    /**
     * 获取最近的任务
     */
    private List<ModelTask> getRecentTasks(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return taskRepository.findRecentSuccessfulTasks(since, PageRequest.of(0, limit));
    }

    /**
     * 查找相似任务用于预热
     */
    private List<ModelTask> findSimilarTasksForWarmup(ModelTask referenceTask, int limit) {
        try {
            List<ModelTask> candidates = taskRepository.findSimilarityCandidates(
                referenceTask.getType(), 
                referenceTask.getComplexity() != null ? referenceTask.getComplexity().toString() : null,
                referenceTask.getOutputFormat() != null ? referenceTask.getOutputFormat().toString() : null,
                PageRequest.of(0, limit * 2)
            );

            return candidates.stream()
                .filter(task -> !task.getTaskId().equals(referenceTask.getTaskId()))
                .filter(task -> {
                    double similarity = calculateSimilarity(referenceTask, task);
                    return similarity >= similarityThreshold;
                })
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding similar tasks for warmup", e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算任务相似度
     */
    private double calculateSimilarity(ModelTask task1, ModelTask task2) {
        try {
            if (task1.getType() == ModelTask.TaskType.TEXT && task2.getType() == ModelTask.TaskType.TEXT) {
                return similarityService.calculateTextSimilarity(
                    task1.getInputText(), task2.getInputText());
            } else if (task1.getType() == ModelTask.TaskType.IMAGE && task2.getType() == ModelTask.TaskType.IMAGE) {
                return similarityService.calculateImageSimilarity(
                    task1.getInputImagePath(), task2.getInputImagePath());
            }
            return 0.0;
        } catch (Exception e) {
            logger.error("Error calculating similarity", e);
            return 0.0;
        }
    }

    /**
     * 检查是否为预热候选
     */
    private boolean isWarmupCandidate(ModelTask task) {
        return task != null 
            && task.getStatus() == ModelTask.TaskStatus.COMPLETED
            && Boolean.TRUE.equals(task.getCached())
            && task.getObjFilePath() != null;
    }

    /**
     * 执行任务预热
     */
    private void performTaskWarmup(ModelTask task) {
        try {
            // 更新最后访问时间，提高缓存优先级
            taskRepository.updateLastAccessed(task.getTaskId(), LocalDateTime.now());
            
            // 可以在这里添加其他预热操作，如预加载文件到内存等
            logger.debug("Warmed up task: {}", task.getTaskId());
        } catch (Exception e) {
            logger.error("Error performing task warmup for task: {}", task.getTaskId(), e);
        }
    }
}