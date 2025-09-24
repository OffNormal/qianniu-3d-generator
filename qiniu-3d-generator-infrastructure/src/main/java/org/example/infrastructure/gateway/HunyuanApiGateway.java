package org.example.infrastructure.gateway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * ��Ԫ3D API����ʵ��
 */
@Slf4j
@Component
public class HunyuanApiGateway {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${hunyuan.api.url:http://localhost:7860}")
    private String apiUrl;
    
    /**
     * �ı�ת3D
     */
    public String textTo3D(String text, Map<String, Object> params) {
        try {
            String url = apiUrl + "/generate/text";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", text);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.getBoolean("success")) {
                    return result.getString("task_id");
                } else {
                    log.error("��Ԫ3D API����ʧ��: {}", result.getString("message"));
                    throw new RuntimeException("API����ʧ��: " + result.getString("message"));
                }
            } else {
                log.error("��Ԫ3D API����ʧ��: {}", response.getBody());
                throw new RuntimeException("API����ʧ��: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("���û�Ԫ3D API�쳣", e);
            throw new RuntimeException("API�����쳣: " + e.getMessage());
        }
    }
    
    /**
     * ͼƬת3D
     */
    public String imageTo3D(String imageData, Map<String, Object> params) {
        try {
            String url = apiUrl + "/generate/image";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image_data", imageData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.getBoolean("success")) {
                    return result.getString("task_id");
                } else {
                    log.error("��Ԫ3D API����ʧ��: {}", result.getString("message"));
                    throw new RuntimeException("API����ʧ��: " + result.getString("message"));
                }
            } else {
                log.error("��Ԫ3D API����ʧ��: {}", response.getBody());
                throw new RuntimeException("API����ʧ��: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("���û�Ԫ3D API�쳣", e);
            throw new RuntimeException("API�����쳣: " + e.getMessage());
        }
    }
    
    /**
     * ��ѯ����״̬
     */
    public TaskResult queryTaskStatus(String taskId) {
        try {
            String url = apiUrl + "/status/" + taskId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                
                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(result.getString("task_id"));
                taskResult.setStatus(result.getString("status"));
                taskResult.setModelUrl(result.getString("model_url"));
                taskResult.setPreviewUrl(result.getString("preview_url"));
                
                // ����״̬���ý���
                String status = result.getString("status");
                if ("completed".equals(status)) {
                    taskResult.setProgress(100);
                } else if ("processing".equals(status)) {
                    taskResult.setProgress(50);
                } else if ("failed".equals(status)) {
                    taskResult.setProgress(0);
                    taskResult.setErrorMessage(result.getString("error"));
                } else {
                    taskResult.setProgress(0);
                }
                
                return taskResult;
            } else {
                log.error("��ѯ����״̬ʧ��: {}", response.getBody());
                throw new RuntimeException("��ѯ����״̬ʧ��: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("��ѯ����״̬�쳣", e);
            throw new RuntimeException("��ѯ����״̬�쳣: " + e.getMessage());
        }
    }
    
    /**
     * �������
     */
    public boolean healthCheck() {
        try {
            String url = apiUrl + "/health";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                return "healthy".equals(result.getString("status"));
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("��Ԫ3D API��������쳣", e);
            return false;
        }
    }
    
    /**
     * ��������
     */
    public static class TaskResult {
        private String taskId;
        private String status;
        private Integer progress;
        private String modelUrl;
        private String previewUrl;
        private String errorMessage;
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        
        public String getModelUrl() { return modelUrl; }
        public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
        
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isCompleted() {
            return "completed".equals(status);
        }
        
        public boolean isFailed() {
            return "failed".equals(status);
        }
        
        public boolean isProcessing() {
            return "processing".equals(status);
        }
    }
}