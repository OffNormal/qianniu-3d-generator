package org.example.infrastructure.gateway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯混元API网关实现
 */
@Slf4j
@Component
public class HunyuanApiGateway {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${hunyuan.api.url:https://hunyuan.tencentcloudapi.com}")
    private String apiUrl;
    
    @Value("${hunyuan.api.secret-id}")
    private String secretId;
    
    @Value("${hunyuan.api.secret-key}")
    private String secretKey;
    
    @Value("${hunyuan.api.region:ap-beijing}")
    private String region;
    
    private static final String SERVICE = "hunyuan";
    private static final String VERSION = "2023-09-01";
    private static final String ACTION_TEXT_TO_3D = "TextTo3D";
    private static final String ACTION_IMAGE_TO_3D = "ImageTo3D";
    private static final String ACTION_QUERY_TASK = "DescribeGenerateModelTask";
    
    /**
     * 文本转3D
     */
    public String textTo3D(String text, Map<String, Object> params) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("Prompt", text);
            requestBody.put("Style", params.getOrDefault("style", "realistic"));
            requestBody.put("Format", params.getOrDefault("format", "obj"));
            
            String response = callTencentApi(ACTION_TEXT_TO_3D, requestBody);
            JSONObject result = JSON.parseObject(response);
            
            if (result.containsKey("Response")) {
                JSONObject responseData = result.getJSONObject("Response");
                if (responseData.containsKey("TaskId")) {
                    return responseData.getString("TaskId");
                } else if (responseData.containsKey("Error")) {
                    JSONObject error = responseData.getJSONObject("Error");
                    throw new RuntimeException("腾讯混元API错误: " + error.getString("Message"));
                }
            }
            
            throw new RuntimeException("API响应格式异常");
            
        } catch (Exception e) {
            log.error("调用腾讯混元文本转3D API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 图片转3D
     */
    public String imageTo3D(String imageUrl, Map<String, Object> params) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ImageUrl", imageUrl);
            requestBody.put("Style", params.getOrDefault("style", "realistic"));
            requestBody.put("Format", params.getOrDefault("format", "obj"));
            
            String response = callTencentApi(ACTION_IMAGE_TO_3D, requestBody);
            JSONObject result = JSON.parseObject(response);
            
            if (result.containsKey("Response")) {
                JSONObject responseData = result.getJSONObject("Response");
                if (responseData.containsKey("TaskId")) {
                    return responseData.getString("TaskId");
                } else if (responseData.containsKey("Error")) {
                    JSONObject error = responseData.getJSONObject("Error");
                    throw new RuntimeException("腾讯混元API错误: " + error.getString("Message"));
                }
            }
            
            throw new RuntimeException("API响应格式异常");
            
        } catch (Exception e) {
            log.error("调用腾讯混元图片转3D API异常", e);
            throw new RuntimeException("API调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务状态
     */
    public TaskResult queryTaskStatus(String taskId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("TaskId", taskId);
            
            String response = callTencentApi(ACTION_QUERY_TASK, requestBody);
            JSONObject result = JSON.parseObject(response);
            
            if (result.containsKey("Response")) {
                JSONObject responseData = result.getJSONObject("Response");
                if (responseData.containsKey("Error")) {
                    JSONObject error = responseData.getJSONObject("Error");
                    throw new RuntimeException("腾讯混元API错误: " + error.getString("Message"));
                }
                
                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(taskId);
                
                String status = responseData.getString("Status");
                taskResult.setStatus(mapStatus(status));
                taskResult.setProgress(responseData.getInteger("Progress"));
                
                if ("SUCCESS".equals(status)) {
                    taskResult.setModelUrl(responseData.getString("ModelUrl"));
                    taskResult.setPreviewUrl(responseData.getString("PreviewUrl"));
                }
                
                if ("FAILED".equals(status)) {
                    taskResult.setErrorMessage(responseData.getString("ErrorMessage"));
                }
                
                return taskResult;
            }
            
            throw new RuntimeException("API响应格式异常");
            
        } catch (Exception e) {
            log.error("查询腾讯混元任务状态异常", e);
            throw new RuntimeException("查询异常: " + e.getMessage());
        }
    }
    
    /**
     * 调用腾讯云API
     */
    private String callTencentApi(String action, Map<String, Object> requestBody) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
        // 构建请求体
        String payload = JSON.toJSONString(requestBody);
        
        // 构建签名
        String authorization = buildAuthorization(action, payload, timestamp, date);
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Host", "hunyuan.tencentcloudapi.com");
        headers.set("X-TC-Action", action);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Version", VERSION);
        headers.set("X-TC-Region", region);
        
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("API调用失败: " + response.getStatusCode());
        }
    }
    
    /**
     * 构建腾讯云API签名
     */
    private String buildAuthorization(String action, String payload, String timestamp, String date) throws Exception {
        // 步骤1：拼接规范请求串
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                                "host:hunyuan.tencentcloudapi.com\n" +
                                "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" +
                                canonicalUri + "\n" +
                                canonicalQueryString + "\n" +
                                canonicalHeaders + "\n" +
                                signedHeaders + "\n" +
                                hashedRequestPayload;
        
        // 步骤2：拼接待签名字符串
        String algorithm = "TC3-HMAC-SHA256";
        String credentialScope = date + "/" + SERVICE + "/" + "tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" +
                            timestamp + "\n" +
                            credentialScope + "\n" +
                            hashedCanonicalRequest;
        
        // 步骤3：计算签名
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, SERVICE);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = bytesToHex(hmac256(secretSigning, stringToSign));
        
        // 步骤4：拼接Authorization
        return algorithm + " " +
               "Credential=" + secretId + "/" + credentialScope + ", " +
               "SignedHeaders=" + signedHeaders + ", " +
               "Signature=" + signature;
    }
    
    /**
     * SHA256哈希
     */
    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }
    
    /**
     * HMAC-SHA256
     */
    private byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 状态映射
     */
    private String mapStatus(String status) {
        switch (status) {
            case "PROCESSING":
                return "PROCESSING";
            case "SUCCESS":
                return "SUCCEEDED";
            case "FAILED":
                return "FAILED";
            default:
                return "UNKNOWN";
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
            return "SUCCEEDED".equals(status);
        }
        
        public boolean isFailed() {
            return "FAILED".equals(status);
        }
    }
}