package org.example.domain.generation.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.domain.generation.repository.IGenerationRepository;
import org.example.domain.generation.service.IGenerationService;
import org.example.types.enums.TaskStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 生成服务实现
 */
@Slf4j
@Service
public class GenerationServiceImpl implements IGenerationService {
    
    @Resource
    private IGenerationRepository generationRepository;
    
    @Override
    public GenerationTaskEntity createTask(GenerationRequest request) {
        log.info("创建生成任务: userId={}, type={}", request.getUserId(), request.getGenerationType());
        
        // 创建任务实体
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(request.getUserId());
        task.setGenerationType(request.getGenerationType());
        task.setInputContent(request.getInputContent());
        task.setInputHash(calculateInputHash(request.getInputContent()));
        task.setStatus(TaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 设置生成参数
        if (request.getParams() != null) {
            task.setGenerationParams(request.getParams().toString());
        }
        
        // 保存任务
        generationRepository.save(task);
        
        log.info("任务创建成功: taskId={}", task.getTaskId());
        return task;
    }
    
    @Override
    public GenerationTaskEntity executeTask(GenerationTaskEntity taskEntity) {
        log.info("执行生成任务: taskId={}", taskEntity.getTaskId());
        
        try {
            // 更新状态为处理中
            taskEntity.setStatus(TaskStatus.PROCESSING);
            taskEntity.setUpdateTime(LocalDateTime.now());
            generationRepository.update(taskEntity);
            
            log.info("任务状态已更新为处理中: taskId={}", taskEntity.getTaskId());
            
        } catch (Exception e) {
            log.error("执行任务失败: taskId={}", taskEntity.getTaskId(), e);
            taskEntity.failProcessing("执行任务异常: " + e.getMessage());
            generationRepository.update(taskEntity);
        }
        
        return taskEntity;
    }
    
    @Override
    public GenerationTaskEntity queryTask(String taskId) {
        log.debug("查询任务: taskId={}", taskId);
        return generationRepository.findByTaskId(taskId);
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        log.info("取消任务: taskId={}", taskId);
        
        try {
            GenerationTaskEntity task = generationRepository.findByTaskId(taskId);
            if (task == null) {
                log.warn("任务不存在: taskId={}", taskId);
                return false;
            }
            
            if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) {
                log.warn("任务已完成或失败，无法取消: taskId={}, status={}", taskId, task.getStatus());
                return false;
            }
            
            // 如果有外部任务ID，记录日志
            if (task.getExternalTaskId() != null) {
                log.info("任务有外部任务ID: externalTaskId={}", task.getExternalTaskId());
            }
            
            // 更新任务状态为失败（取消）
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("任务已被用户取消");
            task.setUpdateTime(LocalDateTime.now());
            generationRepository.update(task);
            
            log.info("任务取消成功: taskId={}", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}", taskId, e);
            return false;
        }
    }
    
    @Override
    public GenerationTaskEntity save(GenerationTaskEntity task) {
        log.debug("保存任务: taskId={}", task.getTaskId());
        generationRepository.save(task);
        return task;
    }
    
    @Override
    public GenerationTaskEntity update(GenerationTaskEntity task) {
        log.debug("更新任务: taskId={}", task.getTaskId());
        task.setUpdateTime(LocalDateTime.now());
        generationRepository.update(task);
        return task;
    }
    
    @Override
    public List<GenerationTaskEntity> queryUserTasks(String userId, int limit) {
        log.debug("查询用户任务列表: userId={}, limit={}", userId, limit);
        return generationRepository.findByUserId(userId, limit);
    }
    

    
    /**
     * 计算输入哈希
     */
    private String calculateInputHash(String inputContent) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(inputContent.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("计算输入哈希失败", e);
            return String.valueOf(inputContent.hashCode());
        }
    }
}