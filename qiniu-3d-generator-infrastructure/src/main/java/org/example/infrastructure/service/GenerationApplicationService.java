package org.example.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.service.ICacheService;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.domain.generation.service.IGenerationService;
import org.example.infrastructure.gateway.MeshyApiGateway;
import org.example.infrastructure.gateway.HunyuanApiGateway;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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
    private HunyuanApiGateway hunyuanApiGateway;
    
    @Value("${generation.api.provider:hunyuan}")
    private String apiProvider;
    
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
            Object result = null;
            
            // 根据配置选择API提供商
            if ("hunyuan".equals(apiProvider)) {
                HunyuanApiGateway.TaskResult hunyuanResult = hunyuanApiGateway.queryTaskStatus(task.getExternalTaskId());
                result = hunyuanResult;
                
                if (hunyuanResult.isCompleted()) {
                    task.completeProcessing(hunyuanResult.getModelUrl(), hunyuanResult.getModelUrl(), hunyuanResult.getPreviewUrl());
                    
                    // 保存到缓存
                    saveToCache(task, hunyuanResult.getModelUrl(), hunyuanResult.getPreviewUrl());
                    
                } else if (hunyuanResult.isFailed()) {
                    task.failProcessing(hunyuanResult.getErrorMessage());
                }
            } else {
                // 默认使用Meshy API
                MeshyApiGateway.TaskResult meshyResult = meshyApiGateway.queryTaskStatus(task.getExternalTaskId());
                result = meshyResult;
                
                if (meshyResult.isCompleted()) {
                    task.completeProcessing(meshyResult.getModelUrl(), meshyResult.getModelUrl(), meshyResult.getPreviewUrl());
                    
                    // 保存到缓存
                    saveToCache(task, meshyResult.getModelUrl(), meshyResult.getPreviewUrl());
                    
                } else if (meshyResult.isFailed()) {
                    task.failProcessing(meshyResult.getErrorMessage());
                }
            }
            
            generationService.update(task);
            
        } catch (Exception e) {
            log.error("更新任务状态失败: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * 保存到缓存的辅助方法
     */
    private void saveToCache(GenerationTaskEntity task, String modelUrl, String previewUrl) {
        try {
            GenerationRequest request = new GenerationRequest();
            request.setInputContent(task.getInputContent());
            request.setGenerationType(task.getGenerationType());
            
            String cacheKey = cacheService.generateCacheKey(request);
            
            // 创建缓存项
            CacheItemEntity cacheItem = CacheItemEntity.builder()
                .cacheKey(cacheKey)
                .inputContent(task.getInputContent())
                .generationType(task.getGenerationType())
                .resultUrl(modelUrl)
                .previewImageUrl(previewUrl)
                .qualityScore(task.getQualityScore())
                .hitCount(0)
                .createTime(java.time.LocalDateTime.now())
                .build();
            
            cacheService.saveCache(cacheItem);
        } catch (Exception e) {
            log.error("保存缓存失败: {}", task.getTaskId(), e);
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
    public void initDatabase() {
        try {
            // 创建generation_task表
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS generation_task (
                    task_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID',
                    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
                    generation_type VARCHAR(32) NOT NULL COMMENT '生成类型',
                    input_content TEXT NOT NULL COMMENT '输入内容',
                    input_hash VARCHAR(64) NOT NULL COMMENT '输入内容哈希',
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                    result_url VARCHAR(512) COMMENT '结果URL',
                    model_file_path VARCHAR(512) COMMENT '模型文件路径',
                    preview_image_url VARCHAR(512) COMMENT '预览图片URL',
                    external_task_id VARCHAR(128) COMMENT '外部任务ID',
                    generation_params JSON COMMENT '生成参数',
                    quality_score DECIMAL(3,2) COMMENT '质量评分',
                    user_rating INT COMMENT '用户评分',
                    processing_time INT COMMENT '处理时间(秒)',
                    error_message TEXT COMMENT '错误信息',
                    from_cache BOOLEAN DEFAULT FALSE COMMENT '是否来自缓存',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    complete_time TIMESTAMP NULL COMMENT '完成时间',
                    INDEX idx_user_id (user_id),
                    INDEX idx_status (status),
                    INDEX idx_input_hash (input_hash),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='3D模型生成任务表'
                """;
            
            jdbcTemplate.execute(createTableSql);
            log.info("数据库初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
}