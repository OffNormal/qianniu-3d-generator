package org.example.domain.generation.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.domain.generation.model.valobj.GenerationRequest;
import org.example.domain.generation.repository.IGenerationRepository;
import org.example.domain.generation.service.IGenerationService;
import org.example.types.enums.GenerationType;
import org.example.types.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ���ɷ���ʵ��
 */
@Slf4j
@Service
public class GenerationServiceImpl implements IGenerationService {

    private final IGenerationRepository generationRepository;

    public GenerationServiceImpl(IGenerationRepository generationRepository) {
        this.generationRepository = generationRepository;
    }
    
    @Override
    public GenerationTaskEntity createTask(GenerationRequest request) {
        log.info("������������: userId={}, type={}", request.getUserId(), request.getGenerationType());
        
        // ��������ʵ��
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(request.getUserId());
        task.setGenerationType(request.getGenerationType());
        task.setInputContent(request.getInputContent());
        task.setInputHash(calculateInputHash(request.getInputContent()));
        task.setStatus(TaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // �������ɲ���
        if (request.getParams() != null) {
            task.setGenerationParams(request.getParams().toString());
        }
        
        // ��������
        generationRepository.save(task);
        
        log.info("���񴴽��ɹ�: taskId={}", task.getTaskId());
        return task;
    }
    
    @Override
    public GenerationTaskEntity executeTask(GenerationTaskEntity task) {
        try {
            // ��������״̬Ϊ������
            task.setStatus(TaskStatus.PROCESSING);
            generationRepository.update(task);
            
            log.info("����ִ�п�ʼ������ID: {}", task.getTaskId());
            
            // ����ֻ����״̬��ʵ�ʵ�API���ý���Ӧ�ò㴦��
            
        } catch (Exception e) {
            log.error("����ִ��ʧ�ܣ�����ID: {}", task.getTaskId(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            generationRepository.update(task);
        }
        return task;
    }
    
    /**
     * �����ⲿAPI��������
     */
    @Override
    public GenerationTaskEntity queryTask(String taskId) {
        log.debug("��ѯ����: taskId={}", taskId);
        return generationRepository.findByTaskId(taskId);
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        log.info("ȡ������: taskId={}", taskId);
        
        try {
            GenerationTaskEntity task = generationRepository.findByTaskId(taskId);
            if (task == null) {
                log.warn("���񲻴���: taskId={}", taskId);
                return false;
            }
            
            if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) {
                log.warn("��������ɻ�ʧ�ܣ��޷�ȡ��: taskId={}, status={}", taskId, task.getStatus());
                return false;
            }
            
            // ������ⲿ����ID����¼��־
            if (task.getExternalTaskId() != null) {
                log.info("�������ⲿ����ID: externalTaskId={}", task.getExternalTaskId());
            }
            
            // ��������״̬Ϊʧ�ܣ�ȡ����
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("�����ѱ��û�ȡ��");
            task.setUpdateTime(LocalDateTime.now());
            generationRepository.update(task);
            
            log.info("����ȡ���ɹ�: taskId={}", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("ȡ������ʧ��: taskId={}", taskId, e);
            return false;
        }
    }
    
    @Override
    public GenerationTaskEntity save(GenerationTaskEntity task) {
        log.debug("��������: taskId={}", task.getTaskId());
        generationRepository.save(task);
        return task;
    }
    
    @Override
    public GenerationTaskEntity update(GenerationTaskEntity task) {
        log.debug("��������: taskId={}", task.getTaskId());
        task.setUpdateTime(LocalDateTime.now());
        generationRepository.update(task);
        return task;
    }
    
    @Override
    public List<GenerationTaskEntity> queryUserTasks(String userId, int limit) {
        log.debug("��ѯ�û������б�: userId={}, limit={}", userId, limit);
        return generationRepository.findByUserId(userId, limit);
    }
    

    
    /**
     * ���������ϣ
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
            log.warn("���������ϣʧ��", e);
            return String.valueOf(inputContent.hashCode());
        }
    }
}