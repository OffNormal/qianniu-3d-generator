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
 * ����Ӧ�÷���
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
     * ������������
     */
    public Response<String> createGenerationTask(String userId, String inputContent, 
                                               GenerationType type, Map<String, Object> params) {
        try {
            // ��黺��
            GenerationRequest request = new GenerationRequest();
            request.setInputContent(inputContent);
            request.setGenerationType(type);
            // request.setParams(params); // ��ʱע�ͣ���Ҫʵ�ֲ�������
            
            String cacheKey = cacheService.generateCacheKey(request);
            CacheItemEntity cacheItem = cacheService.findCache(request);
            
            if (cacheItem != null) {
                log.info("���л��棬ֱ�ӷ��ؽ��: {}", cacheKey);
                return Response.success(cacheItem.getResultUrl());
            }
            
            // �����Ѵ�������������
            request.setUserId(userId);
            // inputContent��generationType�Ѿ����ù���
            // ע�⣺GenerationRequestʹ��GenerationParams������Map
            
            // ��������
            GenerationTaskEntity task = generationService.createTask(request);
            
            // �첽ִ������
            executeTaskAsync(task.getTaskId());
            
            return Response.success(task.getTaskId());
            
        } catch (Exception e) {
            log.error("������������ʧ��", e);
            return Response.fail("��������ʧ��: " + e.getMessage());
        }
    }
    
    /**
     * ��ѯ����״̬
     */
    public Response<GenerationTaskEntity> queryTask(String taskId) {
        try {
            GenerationTaskEntity task = generationService.queryTask(taskId);
            if (task == null) {
                return Response.fail("���񲻴���");
            }
            
            // ����������ڴ����У���ѯ�ⲿAPI״̬
            if (task.getStatus() == TaskStatus.PROCESSING && task.getExternalTaskId() != null) {
                updateTaskFromExternalApi(task);
            }
            
            return Response.success(task);
            
        } catch (Exception e) {
            log.error("��ѯ����ʧ��", e);
            return Response.fail("��ѯʧ��: " + e.getMessage());
        }
    }
    
    /**
     * ��ȡ�û������б�
     */
    public Response<List<GenerationTaskEntity>> getUserTasks(String userId, int limit) {
        try {
            List<GenerationTaskEntity> tasks = generationService.queryUserTasks(userId, limit);
            return Response.success(tasks);
            
        } catch (Exception e) {
            log.error("��ȡ�û������б�ʧ��", e);
            return Response.fail("��ȡʧ��: " + e.getMessage());
        }
    }
    
    /**
     * ȡ������
     */
    public Response<Void> cancelTask(String taskId) {
        try {
            generationService.cancelTask(taskId);
            return Response.success(null);
            
        } catch (Exception e) {
            log.error("ȡ������ʧ��", e);
            return Response.fail("ȡ��ʧ��: " + e.getMessage());
        }
    }
    
    /**
     * �첽ִ������
     */
    private void executeTaskAsync(String taskId) {
        new Thread(() -> {
            try {
                GenerationTaskEntity task = generationService.queryTask(taskId);
                if (task != null) {
                    generationService.executeTask(task);
                }
            } catch (Exception e) {
                log.error("ִ�������쳣: {}", taskId, e);
            }
        }).start();
    }
    
    /**
     * ���ⲿAPI��������״̬
     */
    private void updateTaskFromExternalApi(GenerationTaskEntity task) {
        try {
            Object result = null;
            
            // ��������ѡ��API�ṩ��
            if ("hunyuan".equals(apiProvider)) {
                HunyuanApiGateway.TaskResult hunyuanResult = hunyuanApiGateway.queryTaskStatus(task.getExternalTaskId());
                result = hunyuanResult;
                
                if (hunyuanResult.isCompleted()) {
                    task.completeProcessing(hunyuanResult.getModelUrl(), hunyuanResult.getModelUrl(), hunyuanResult.getPreviewUrl());
                    
                    // ���浽����
                    saveToCache(task, hunyuanResult.getModelUrl(), hunyuanResult.getPreviewUrl());
                    
                } else if (hunyuanResult.isFailed()) {
                    task.failProcessing(hunyuanResult.getErrorMessage());
                }
            } else {
                // Ĭ��ʹ��Meshy API
                MeshyApiGateway.TaskResult meshyResult = meshyApiGateway.queryTaskStatus(task.getExternalTaskId());
                result = meshyResult;
                
                if (meshyResult.isCompleted()) {
                    task.completeProcessing(meshyResult.getModelUrl(), meshyResult.getModelUrl(), meshyResult.getPreviewUrl());
                    
                    // ���浽����
                    saveToCache(task, meshyResult.getModelUrl(), meshyResult.getPreviewUrl());
                    
                } else if (meshyResult.isFailed()) {
                    task.failProcessing(meshyResult.getErrorMessage());
                }
            }
            
            generationService.update(task);
            
        } catch (Exception e) {
            log.error("��������״̬ʧ��: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * ���浽����ĸ�������
     */
    private void saveToCache(GenerationTaskEntity task, String modelUrl, String previewUrl) {
        try {
            GenerationRequest request = new GenerationRequest();
            request.setInputContent(task.getInputContent());
            request.setGenerationType(task.getGenerationType());
            
            String cacheKey = cacheService.generateCacheKey(request);
            
            // ����������
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
            log.error("���滺��ʧ��: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * ��������
     */
    private Map<String, Object> parseParams(String paramsJson) {
        // ��ʵ�֣�ʵ��Ӧ����JSON����
        return new HashMap<>();
    }
    
    /**
     * ���ݿ��ʼ��
     */
    public void initDatabase() {
        try {
            // ����generation_task��
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS generation_task (
                    task_id VARCHAR(64) PRIMARY KEY COMMENT '����ID',
                    user_id VARCHAR(64) NOT NULL COMMENT '�û�ID',
                    generation_type VARCHAR(32) NOT NULL COMMENT '��������',
                    input_content TEXT NOT NULL COMMENT '��������',
                    input_hash VARCHAR(64) NOT NULL COMMENT '�������ݹ�ϣ',
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '����״̬',
                    result_url VARCHAR(512) COMMENT '���URL',
                    model_file_path VARCHAR(512) COMMENT 'ģ���ļ�·��',
                    preview_image_url VARCHAR(512) COMMENT 'Ԥ��ͼƬURL',
                    external_task_id VARCHAR(128) COMMENT '�ⲿ����ID',
                    generation_params JSON COMMENT '���ɲ���',
                    quality_score DECIMAL(3,2) COMMENT '��������',
                    user_rating INT COMMENT '�û�����',
                    processing_time INT COMMENT '����ʱ��(��)',
                    error_message TEXT COMMENT '������Ϣ',
                    from_cache BOOLEAN DEFAULT FALSE COMMENT '�Ƿ����Ի���',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '����ʱ��',
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '����ʱ��',
                    complete_time TIMESTAMP NULL COMMENT '���ʱ��',
                    INDEX idx_user_id (user_id),
                    INDEX idx_status (status),
                    INDEX idx_input_hash (input_hash),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='3Dģ�����������'
                """;
            
            jdbcTemplate.execute(createTableSql);
            log.info("���ݿ��ʼ�����");
        } catch (Exception e) {
            log.error("���ݿ��ʼ��ʧ��", e);
            throw new RuntimeException("���ݿ��ʼ��ʧ��", e);
        }
    }
}