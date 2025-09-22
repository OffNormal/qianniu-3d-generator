package org.example.infrastructure.repository;

import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.repository.IGenerationRepository;
import org.example.infrastructure.persistent.dao.IGenerationTaskDao;
import org.example.infrastructure.persistent.po.GenerationTaskPO;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成任务仓储实现
 */
@Repository
public class GenerationRepositoryImpl implements IGenerationRepository {
    
    @Resource
    private IGenerationTaskDao generationTaskDao;
    
    @Override
    public void save(GenerationTaskEntity entity) {
        GenerationTaskPO po = convertToPO(entity);
        if (generationTaskDao.selectByTaskId(entity.getTaskId()) == null) {
            generationTaskDao.insert(po);
        } else {
            generationTaskDao.update(po);
        }
    }
    
    @Override
    public void update(GenerationTaskEntity entity) {
        GenerationTaskPO po = convertToPO(entity);
        generationTaskDao.update(po);
    }
    
    @Override
    public GenerationTaskEntity findByTaskId(String taskId) {
        GenerationTaskPO po = generationTaskDao.selectByTaskId(taskId);
        return po != null ? convertToEntity(po) : null;
    }
    
    @Override
    public List<GenerationTaskEntity> findByUserId(String userId, int limit) {
        List<GenerationTaskPO> poList = generationTaskDao.selectByUserId(userId, limit);
        return poList.stream().map(this::convertToEntity).collect(Collectors.toList());
    }
    
    @Override
    public GenerationTaskEntity findByInputHash(String inputHash) {
        GenerationTaskPO po = generationTaskDao.selectByInputHash(inputHash);
        return po != null ? convertToEntity(po) : null;
    }
    
    @Override
    public List<GenerationTaskEntity> findSimilarCompletedTasks(String inputContent, double similarity) {
        List<GenerationTaskPO> poList = generationTaskDao.selectSimilarCompletedTasks(inputContent, similarity);
        return poList.stream().map(this::convertToEntity).collect(Collectors.toList());
    }
    
    /**
     * 转换为PO
     */
    private GenerationTaskPO convertToPO(GenerationTaskEntity entity) {
        return GenerationTaskPO.builder()
                .taskId(entity.getTaskId())
                .userId(entity.getUserId())
                .generationType(entity.getGenerationType().name())
                .inputContent(entity.getInputContent())
                .inputHash(entity.getInputHash())
                .status(entity.getStatus().name())
                .resultUrl(entity.getResultUrl())
                .modelFilePath(entity.getModelFilePath())
                .previewImageUrl(entity.getPreviewImageUrl())
                .externalTaskId(entity.getExternalTaskId())
                .generationParams(entity.getGenerationParams())
                .qualityScore(entity.getQualityScore())
                .userRating(entity.getUserRating())
                .processingTime(entity.getProcessingTime())
                .errorMessage(entity.getErrorMessage())
                .fromCache(entity.getFromCache())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .completeTime(entity.getCompleteTime())
                .build();
    }
    
    /**
     * 转换为Entity
     */
    private GenerationTaskEntity convertToEntity(GenerationTaskPO po) {
        GenerationTaskEntity entity = new GenerationTaskEntity();
        entity.setTaskId(po.getTaskId());
        entity.setUserId(po.getUserId());
        entity.setGenerationType(GenerationType.valueOf(po.getGenerationType()));
        entity.setInputContent(po.getInputContent());
        entity.setInputHash(po.getInputHash());
        entity.setStatus(TaskStatus.valueOf(po.getStatus()));
        entity.setResultUrl(po.getResultUrl());
        entity.setModelFilePath(po.getModelFilePath());
        entity.setPreviewImageUrl(po.getPreviewImageUrl());
        entity.setExternalTaskId(po.getExternalTaskId());
        entity.setGenerationParams(po.getGenerationParams());
        entity.setQualityScore(po.getQualityScore());
        entity.setUserRating(po.getUserRating());
        entity.setProcessingTime(po.getProcessingTime());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setFromCache(po.getFromCache());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateTime(po.getUpdateTime());
        entity.setCompleteTime(po.getCompleteTime());
        return entity;
    }
}