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
 * Meshy API网关实现
 */
@Slf4j
@Component
public class MeshyApiGateway {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${meshy.api.url:https://api.meshy.ai}")
    private String apiUrl;
    
    @Value("${meshy.api.key}")
    private String apiKey;
    
    /**
     * 文本转3D
     */
    public String textTo3D(String text, Map<String, Object> params) {
        try {
            String url = apiUrl + "/v1/text-to-3d";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("mode", "preview");
            requestBody.put("prompt", text);
            requestBody.put("art_style", params.getOrDefault("style", "realistic"));
            requestBody.put("negative_prompt", "low quality, blurry");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                return result.getString("result");
            } else {
                log.error("Meshy API调用失败: {}", response.getBody());
                throw new RuntimeException("API调用失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用Meshy API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 图片转3D
     */
    public String imageTo3D(String imageUrl, Map<String, Object> params) {
        try {
            String url = apiUrl + "/v1/image-to-3d";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("mode", "preview");
            requestBody.put("image_url", imageUrl);
            requestBody.put("enable_pbr", params.getOrDefault("generateTexture", true));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                return result.getString("result");
            } else {
                log.error("Meshy API调用失败: {}", response.getBody());
                throw new RuntimeException("API调用失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用Meshy API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务状态
     */
    public TaskResult queryTaskStatus(String taskId) {
        try {
            String url = apiUrl + "/v1/text-to-3d/" + taskId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSON.parseObject(response.getBody());
                
                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(taskId);
                taskResult.setStatus(result.getString("status"));
                taskResult.setProgress(result.getInteger("progress"));
                
                if ("SUCCEEDED".equals(taskResult.getStatus())) {
                    taskResult.setModelUrl(result.getString("model_url"));
                    taskResult.setPreviewUrl(result.getString("thumbnail_url"));
                }
                
                if ("FAILED".equals(taskResult.getStatus())) {
                    taskResult.setErrorMessage(result.getString("error"));
                }
                
                return taskResult;
            } else {
                log.error("查询任务状态失败: {}", response.getBody());
                throw new RuntimeException("查询失败: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("查询任务状态异常", e);
            throw new RuntimeException("查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 任务结果
     */
    public static class TaskResult {
        private String taskId;
        private String status;
        private Integer progress;
        private String modelUrl;
        private String previewUrl;
        private String errorMessage;
        
        // getters and setters
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
            return "SUCCEEDED".equals(status);
        }
        
        public boolean isFailed() {
            return "FAILED".equals(status);
        }
        
        public boolean isProcessing() {
            return "IN_PROGRESS".equals(status) || "PENDING".equals(status);
        }
    }
}