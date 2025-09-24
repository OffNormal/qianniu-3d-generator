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
 * 混元3D API网关实现
 */
@Slf4j
@Component
public class HunyuanApiGateway {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${hunyuan.api.url:http://localhost:7860}")
    private String apiUrl;
    
    /**
     * 文本转3D
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
                    log.error("混元3D API调用失败: {}", result.getString("message"));
                    throw new RuntimeException("API调用失败: " + result.getString("message"));
                }
            } else {
                log.error("混元3D API调用失败: {}", response.getBody());
                throw new RuntimeException("API调用失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用混元3D API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 图片转3D
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
                    log.error("混元3D API调用失败: {}", result.getString("message"));
                    throw new RuntimeException("API调用失败: " + result.getString("message"));
                }
            } else {
                log.error("混元3D API调用失败: {}", response.getBody());
                throw new RuntimeException("API调用失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用混元3D API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务状态
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
                
                // 根据状态设置进度
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
                log.error("查询任务状态失败: {}", response.getBody());
                throw new RuntimeException("查询任务状态失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("查询任务状态异常", e);
            throw new RuntimeException("查询任务状态异常: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
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
            log.error("混元3D API健康检查异常", e);
            return false;
        }
    }
    
    /**
     * 任务结果类
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