package org.example.domain.generation.repository;

import org.example.domain.generation.model.entity.GenerationTaskEntity;

import java.util.List;

/**
 * 生成任务仓储接口
 */
public interface IGenerationRepository {
    
    /**
     * 保存任务
     * @param taskEntity 任务实体
     */
    void save(GenerationTaskEntity taskEntity);
    
    /**
     * 更新任务
     * @param taskEntity 任务实体
     */
    void update(GenerationTaskEntity taskEntity);
    
    /**
     * 根据任务ID查询
     * @param taskId 任务ID
     * @return 任务实体
     */
    GenerationTaskEntity findByTaskId(String taskId);
    
    /**
     * 根据用户ID查询任务列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 任务列表
     */
    List<GenerationTaskEntity> findByUserId(String userId, int limit);
    
    /**
     * 根据输入哈希查询缓存
     * @param inputHash 输入哈希
     * @return 任务实体
     */
    GenerationTaskEntity findByInputHash(String inputHash);
    
    /**
     * 查询相似的已完成任务（用于缓存匹配）
     * @param inputContent 输入内容
     * @param similarity 相似度阈值
     * @return 相似任务列表
     */
    List<GenerationTaskEntity> findSimilarCompletedTasks(String inputContent, double similarity);
}