package org.example.domain.generation.service;

import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;

/**
 * 3D模型生成领域服务接口
 */
public interface IGenerationService {
    
    /**
     * 创建生成任务
     * @param request 生成请求
     * @return 任务实体
     */
    GenerationTaskEntity createTask(GenerationRequest request);
    
    /**
     * 执行生成任务
     * @param taskEntity 任务实体
     * @return 更新后的任务实体
     */
    GenerationTaskEntity executeTask(GenerationTaskEntity taskEntity);
    
    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务实体
     */
    GenerationTaskEntity queryTask(String taskId);
    
    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);
    
    /**
     * 保存任务
     * @param task 任务实体
     * @return 保存后的任务实体
     */
    GenerationTaskEntity save(GenerationTaskEntity task);
    
    /**
     * 更新任务
     * @param task 任务实体
     * @return 更新后的任务实体
     */
    GenerationTaskEntity update(GenerationTaskEntity task);
    
    /**
     * 查询用户任务列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 任务列表
     */
    java.util.List<GenerationTaskEntity> queryUserTasks(String userId, int limit);
}