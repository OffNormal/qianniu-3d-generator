package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.cache.model.entity.CacheItemEntity;
import org.example.domain.cache.service.ICacheService;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.domain.generation.service.IGenerationService;
import org.example.infrastructure.gateway.ApiProviderFactory;
import org.example.infrastructure.gateway.MeshyApiGateway;
import org.example.infrastructure.gateway.HunyuanApiGateway;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
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
    private ApiProviderFactory apiProviderFactory;
    
    @Resource
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 创建生成任务
     */
    public Response<String> createGenerationTask(String userId, String inputContent, 
                                               GenerationType type, Map<String, Object> params) {
        return createGenerationTask(userId, inputContent, type, params, null);
    }
    
    /**
     * 创建生成任务（指定API提供商）
     */
    public Response<String> createGenerationTask(String userId, String inputContent, 
                                               GenerationType type, Map<String, Object> params, String apiProvider) {
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
            
            // 如果指定了API提供商，设置到任务中
            if (apiProvider != null && !apiProvider.trim().isEmpty()) {
                // 验证API提供商是否可用
                if (!Arrays.asList(apiProviderFactory.getAvailableProviders()).contains(apiProvider)) {
                    return Response.fail("不支持的API提供商: " + apiProvider);
                }
                task.setApiProvider(apiProvider);
                generationService.update(task);
            }
            
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
                    // 更新任务状态为处理中
                    generationService.executeTask(task);
                    
                    // 调用外部API
                    callExternalApi(task);
                }
            } catch (Exception e) {
                log.error("执行任务异常: {}", taskId, e);
                // 更新任务状态为失败
                GenerationTaskEntity task = generationService.queryTask(taskId);
                if (task != null) {
                    task.failProcessing("执行任务异常: " + e.getMessage());
                    generationService.update(task);
                }
            }
        }).start();
    }
    
    /**
     * 调用外部API
     */
    private void callExternalApi(GenerationTaskEntity task) {
        try {
            String externalTaskId;
            String provider = task.getApiProvider() != null ? task.getApiProvider() : apiProviderFactory.getDefaultProvider();
            
            // 根据生成类型调用不同的API
            if (task.getGenerationType() == GenerationType.TEXT_TO_3D) {
                externalTaskId = apiProviderFactory.generateFromText(task.getInputContent(), provider);
            } else if (task.getGenerationType() == GenerationType.IMAGE_TO_3D) {
                externalTaskId = apiProviderFactory.generateFromImage(task.getInputContent(), provider);
            } else {
                throw new IllegalArgumentException("不支持的生成类型: " + task.getGenerationType());
            }
            
            // 更新任务的外部任务ID和API提供商
            task.setExternalTaskId(externalTaskId);
            task.setApiProvider(provider);
            generationService.update(task);
            
            log.info("外部API调用成功: taskId={}, externalTaskId={}, provider={}", 
                    task.getTaskId(), externalTaskId, provider);
            
        } catch (Exception e) {
            log.error("调用外部API失败: taskId={}", task.getTaskId(), e);
            
            // 如果启用了fallback，尝试备用提供商
            if (apiProviderFactory.isFallbackEnabled()) {
                try {
                    String fallbackProvider = apiProviderFactory.getFallbackProvider();
                    log.info("尝试使用备用提供商: {}", fallbackProvider);
                    
                    String externalTaskId;
                    if (task.getGenerationType() == GenerationType.TEXT_TO_3D) {
                        externalTaskId = apiProviderFactory.generateFromText(task.getInputContent(), fallbackProvider);
                    } else {
                        externalTaskId = apiProviderFactory.generateFromImage(task.getInputContent(), fallbackProvider);
                    }
                    
                    task.setExternalTaskId(externalTaskId);
                    task.setApiProvider(fallbackProvider);
                    generationService.update(task);
                    
                    log.info("备用API调用成功: taskId={}, externalTaskId={}, provider={}", 
                            task.getTaskId(), externalTaskId, fallbackProvider);
                    
                } catch (Exception fallbackException) {
                    log.error("备用API调用也失败: taskId={}", task.getTaskId(), fallbackException);
                    task.failProcessing("所有API提供商都调用失败: " + e.getMessage() + "; " + fallbackException.getMessage());
                    generationService.update(task);
                }
            } else {
                task.failProcessing("外部API调用失败: " + e.getMessage());
                generationService.update(task);
            }
        }
    }
    
    /**
     * 从外部API更新任务状态
     */
    private void updateTaskFromExternalApi(GenerationTaskEntity task) {
        try {
            String provider = task.getApiProvider() != null ? task.getApiProvider() : apiProviderFactory.getDefaultProvider();
            Object result = apiProviderFactory.queryTaskStatus(task.getExternalTaskId(), provider);
            
            boolean isCompleted = false;
            boolean isFailed = false;
            String modelUrl = null;
            String previewUrl = null;
            String errorMessage = null;
            
            // 根据不同的API提供商处理结果
            if ("meshy".equals(provider) && result instanceof MeshyApiGateway.TaskResult) {
                MeshyApiGateway.TaskResult meshyResult = (MeshyApiGateway.TaskResult) result;
                isCompleted = meshyResult.isCompleted();
                isFailed = meshyResult.isFailed();
                modelUrl = meshyResult.getModelUrl();
                previewUrl = meshyResult.getPreviewUrl();
                errorMessage = meshyResult.getErrorMessage();
            } else if ("hunyuan".equals(provider) && result instanceof HunyuanApiGateway.TaskResult) {
                HunyuanApiGateway.TaskResult hunyuanResult = (HunyuanApiGateway.TaskResult) result;
                isCompleted = hunyuanResult.isCompleted();
                isFailed = hunyuanResult.isFailed();
                modelUrl = hunyuanResult.getModelUrl();
                previewUrl = hunyuanResult.getPreviewUrl();
                errorMessage = hunyuanResult.getErrorMessage();
            }
            
            if (isCompleted) {
                task.completeProcessing(modelUrl, modelUrl, previewUrl);
                
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
                    .resultUrl(modelUrl)
                    .previewImageUrl(previewUrl)
                    .qualityScore(task.getQualityScore())
                    .hitCount(0)
                    .createTime(java.time.LocalDateTime.now())
                    .build();
                
                cacheService.saveCache(cacheItem);
                
            } else if (isFailed) {
                task.failProcessing(errorMessage);
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
     * 获取可用的API提供商信息
     */
    public Response<Map<String, Object>> getApiProviders() {
        try {
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("availableProviders", apiProviderFactory.getAvailableProviders());
            providerInfo.put("defaultProvider", apiProviderFactory.getDefaultProvider());
            providerInfo.put("fallbackProvider", apiProviderFactory.getFallbackProvider());
            providerInfo.put("fallbackEnabled", apiProviderFactory.isFallbackEnabled());
            
            return Response.success(providerInfo);
            
        } catch (Exception e) {
            log.error("获取API提供商信息失败", e);
            return Response.fail("获取失败: " + e.getMessage());
        }
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