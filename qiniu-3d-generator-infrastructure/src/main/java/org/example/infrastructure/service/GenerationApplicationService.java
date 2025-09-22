package org.example.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.service.ICacheService;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.domain.generation.service.IGenerationService;
import org.example.infrastructure.gateway.MeshyApiGateway;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 生成应用服务
 */
@Slf4j
@Service
public class GenerationApplicationService {
    
    @Resource
    private IGenerationService generationService;
    
    @Resource
    private ICacheService cacheService;
    
    @Resource
    private MeshyApiGateway meshyApiGateway;
    
    @Resource
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 创建生成任务
     */
    public Response<String> createGenerationTask(String userId, String inputContent, 
                                               GenerationType type, Map<String, Object> params) {
        try {
            // 检查缓存
            GenerationRequest request = new GenerationRequest();
            request.setInputContent(inputContent);
            request.setGenerationType(type);
            // request.setParams(params); // 暂时注释，需要实现参数设置
            
            String cacheKey = cacheService.generateCacheKey(request);
            CacheItemEntity cacheItem = cacheService.findCache(request);
            
            if (cacheItem != null) {
                log.info("命中缓存，直接返回结果: {}", cacheKey);
                return Response.success(cacheItem.getResultUrl());
            }
            
            // 重用已创建的生成请求
            request.setUserId(userId);
            // inputContent和generationType已经设置过了
            // 注意：GenerationRequest使用GenerationParams而不是Map
            
            // 创建任务
            GenerationTaskEntity task = generationService.createTask(request);
            
            // 异步执行任务
            executeTaskAsync(task.getTaskId());
            
            return Response.success(task.getTaskId());
            
        } catch (Exception e) {
            log.error("创建生成任务失败", e);
            return Response.fail("创建任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务状态
     */
    public Response<GenerationTaskEntity> queryTask(String taskId) {
        try {
            GenerationTaskEntity task = generationService.queryTask(taskId);
            if (task == null) {
                return Response.fail("任务不存在");
            }
            
            // 如果任务正在处理中，查询外部API状态
            if (task.getStatus() == TaskStatus.PROCESSING && task.getExternalTaskId() != null) {
                updateTaskFromExternalApi(task);
            }
            
            return Response.success(task);
            
        } catch (Exception e) {
            log.error("查询任务失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户任务列表
     */
    public Response<List<GenerationTaskEntity>> getUserTasks(String userId, int limit) {
        try {
            List<GenerationTaskEntity> tasks = generationService.queryUserTasks(userId, limit);
            return Response.success(tasks);
            
        } catch (Exception e) {
            log.error("获取用户任务列表失败", e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消任务
     */
    public Response<Void> cancelTask(String taskId) {
        try {
            generationService.cancelTask(taskId);
            return Response.success(null);
            
        } catch (Exception e) {
            log.error("取消任务失败", e);
            return Response.fail("取消失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步执行任务
     */
    private void executeTaskAsync(String taskId) {
        new Thread(() -> {
            try {
                GenerationTaskEntity task = generationService.queryTask(taskId);
                if (task != null) {
                    generationService.executeTask(task);
                }
            } catch (Exception e) {
                log.error("执行任务异常: {}", taskId, e);
            }
        }).start();
    }
    
    /**
     * 从外部API更新任务状态
     */
    private void updateTaskFromExternalApi(GenerationTaskEntity task) {
        try {
            MeshyApiGateway.TaskResult result = meshyApiGateway.queryTaskStatus(task.getExternalTaskId());
            
            if (result.isCompleted()) {
                task.completeProcessing(result.getModelUrl(), result.getModelUrl(), result.getPreviewUrl());
                
                // 保存到缓存
                GenerationRequest request = new GenerationRequest();
                request.setInputContent(task.getInputContent());
                request.setGenerationType(task.getGenerationType());
                
                String cacheKey = cacheService.generateCacheKey(request);
                
                // 创建缓存项
                CacheItemEntity cacheItem = CacheItemEntity.builder()
                    .cacheKey(cacheKey)
                    .inputContent(task.getInputContent())
                    .generationType(task.getGenerationType())
                    .resultUrl(result.getModelUrl())
                    .previewImageUrl(result.getPreviewUrl())
                    .qualityScore(task.getQualityScore())
                    .hitCount(0)
                    .createTime(java.time.LocalDateTime.now())
                    .build();
                
                cacheService.saveCache(cacheItem);
                
            } else if (result.isFailed()) {
                task.failProcessing(result.getErrorMessage());
            }
            
            generationService.update(task);
            
        } catch (Exception e) {
            log.error("更新任务状态失败: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * 解析参数
     */
    private Map<String, Object> parseParams(String paramsJson) {
        // 简单实现，实际应该用JSON解析
        return new HashMap<>();
    }
    
    /**
     * 数据库初始化
     */
    public Response<String> initDatabase() {
        try {
            // 创建generation_task表
            String createGenerationTaskTable = "CREATE TABLE IF NOT EXISTS generation_task (" +
                "task_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID'," +
                "user_id VARCHAR(64) NOT NULL COMMENT '用户ID'," +
                "generation_type VARCHAR(20) NOT NULL COMMENT '生成类型：TEXT/IMAGE'," +
                "input_content TEXT NOT NULL COMMENT '输入内容'," +
                "input_hash VARCHAR(64) NOT NULL COMMENT '输入内容哈希'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/COMPLETED/FAILED/CACHED'," +
                "result_url VARCHAR(500) COMMENT '生成结果URL'," +
                "model_file_path VARCHAR(500) COMMENT '模型文件路径'," +
                "preview_image_url VARCHAR(500) COMMENT '预览图URL'," +
                "external_task_id VARCHAR(100) COMMENT '第三方API任务ID'," +
                "generation_params TEXT COMMENT '生成参数JSON'," +
                "quality_score DECIMAL(3,2) COMMENT '质量评分(0-10)'," +
                "user_rating INT COMMENT '用户评分(1-10)'," +
                "processing_time BIGINT COMMENT '处理耗时(毫秒)'," +
                "error_message TEXT COMMENT '错误信息'," +
                "from_cache BOOLEAN DEFAULT FALSE COMMENT '是否来自缓存'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "complete_time DATETIME COMMENT '完成时间'," +
                "INDEX idx_user_id (user_id)," +
                "INDEX idx_status (status)," +
                "INDEX idx_input_hash (input_hash)," +
                "INDEX idx_create_time (create_time)," +
                "INDEX idx_quality_score (quality_score)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务表'";
            
            jdbcTemplate.execute(createGenerationTaskTable);
            log.info("数据库表创建成功");
            return Response.success("数据库初始化成功");
            
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            return Response.fail("数据库初始化失败: " + e.getMessage());
        }
    }
}