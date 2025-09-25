package org.example.infrastructure.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import org.example.infrastructure.config.TencentCloudConfig;
import org.example.domain.generation.service.IHunyuanService;
import org.example.types.model.HunyuanRequest;
import org.example.types.model.HunyuanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class HunyuanServiceImpl implements IHunyuanService {

    private static final Logger logger = LoggerFactory.getLogger(HunyuanServiceImpl.class);

    @Autowired
    private TencentCloudConfig tencentCloudConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Credential credential;
    private ClientProfile clientProfile;
    
    @PostConstruct
    public void init() {
        try {
            // 初始化认证信息
            this.credential = new Credential(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
            );

            // 配置HTTP Profile
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(tencentCloudConfig.getHunyuan().getEndpoint());
            httpProfile.setConnTimeout(tencentCloudConfig.getHunyuan().getTimeout());
            httpProfile.setReadTimeout(tencentCloudConfig.getHunyuan().getTimeout());

            // 配置Client Profile
            this.clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            logger.info("Hunyuan Service initialized successfully with endpoint: {}", 
                       tencentCloudConfig.getHunyuan().getEndpoint());
        } catch (Exception e) {
            logger.error("Failed to initialize Hunyuan Service", e);
            throw new RuntimeException("Failed to initialize Hunyuan Service", e);
        }
    }
    
    @Override
    public HunyuanResponse submitHunyuanTo3DJob(HunyuanRequest request) {
        try {
            // 验证请求参数
            validateRequest(request);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            
            if (request.getPrompt() != null) {
                requestBody.put("Prompt", request.getPrompt());
            }
            
            if (request.getImageBase64() != null) {
                requestBody.put("ImageBase64", request.getImageBase64());
            }
            
            if (request.getImageUrl() != null) {
                requestBody.put("ImageUrl", request.getImageUrl());
            }
            
            if (request.getMultiViewImages() != null && !request.getMultiViewImages().isEmpty()) {
                List<Map<String, String>> viewImages = new ArrayList<>();
                for (HunyuanRequest.ViewImage img : request.getMultiViewImages()) {
                    Map<String, String> viewImage = new HashMap<>();
                    if (img.getImageBase64() != null) {
                        viewImage.put("ImageBase64", img.getImageBase64());
                    }
                    if (img.getImageUrl() != null) {
                        viewImage.put("ImageUrl", img.getImageUrl());
                    }
                    if (img.getView() != null) {
                        viewImage.put("View", img.getView());
                    }
                    viewImages.add(viewImage);
                }
                requestBody.put("MultiViewImages", viewImages);
            }
            
            if (request.getResultFormat() != null) {
                requestBody.put("ResultFormat", request.getResultFormat());
            } else {
                requestBody.put("ResultFormat", "OBJ"); // 默认格式
            }
            
            if (request.getEnablePBR() != null) {
                requestBody.put("EnablePBR", request.getEnablePBR());
            }

            // 发送HTTP请求
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpHeaders headers = buildHeaders("SubmitHunyuanTo3DJob", jsonBody);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            String url = "https://" + tencentCloudConfig.getHunyuan().getEndpoint() + "/";
            
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
            Map<String, Object> response = (Map<String, Object>) responseMap.get("Response");

            HunyuanResponse result = new HunyuanResponse();
            
            if (response.containsKey("Error")) {
                Map<String, String> error = (Map<String, String>) response.get("Error");
                HunyuanResponse.ErrorInfo errorInfo = HunyuanResponse.ErrorInfo.builder()
                    .code(error.get("Code"))
                    .message(error.get("Message"))
                    .build();
                
                HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                    .error(errorInfo)
                    .build();
                result.setResponse(responseData);
            } else {
                HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                    .jobId((String) response.get("JobId"))
                    .requestId((String) response.get("RequestId"))
                    .build();
                result.setResponse(responseData);
            }

            logger.info("Successfully submitted Hunyuan 3D job, JobId: {}, RequestId: {}", 
                       result.getResponse() != null ? result.getResponse().getJobId() : null,
                       result.getResponse() != null ? result.getResponse().getRequestId() : null);

            return result;

        } catch (Exception e) {
            logger.error("Error when submitting Hunyuan 3D job", e);
            
            HunyuanResponse errorResponse = new HunyuanResponse();
            HunyuanResponse.ErrorInfo errorInfo = HunyuanResponse.ErrorInfo.builder()
                .code("InternalError")
                .message("Internal server error: " + e.getMessage())
                .build();
            
            HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                .error(errorInfo)
                .build();
            errorResponse.setResponse(responseData);
            
            return errorResponse;
        }
    }
    
    @Override
    public HunyuanResponse queryJobStatus(String jobId) {
        try {
            if (jobId == null || jobId.trim().isEmpty()) {
                throw new IllegalArgumentException("JobId cannot be null or empty");
            }

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("JobId", jobId);

            // 发送HTTP请求
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpHeaders headers = buildHeaders("DescribeHunyuanTo3DJob", jsonBody);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            String url = "https://" + tencentCloudConfig.getHunyuan().getEndpoint() + "/";
            
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            // 解析响应
            Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
            Map<String, Object> response = (Map<String, Object>) responseMap.get("Response");

            HunyuanResponse result = new HunyuanResponse();
            
            if (response.containsKey("Error")) {
                Map<String, String> error = (Map<String, String>) response.get("Error");
                HunyuanResponse.ErrorInfo errorInfo = HunyuanResponse.ErrorInfo.builder()
                    .code(error.get("Code"))
                    .message(error.get("Message"))
                    .build();
                
                HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                    .error(errorInfo)
                    .build();
                result.setResponse(responseData);
            } else {
                HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                    .jobId((String) response.get("JobId"))
                    .requestId((String) response.get("RequestId"))
                    .status((String) response.get("Status"))
                    .resultUrl((String) response.get("ResultUrl"))
                    .build();
                result.setResponse(responseData);
            }

            logger.info("Successfully queried Hunyuan 3D job status, JobId: {}, Status: {}", 
                       jobId, result.getResponse() != null ? result.getResponse().getStatus() : null);

            return result;

        } catch (Exception e) {
            logger.error("Error when querying Hunyuan 3D job status", e);
            
            HunyuanResponse errorResponse = new HunyuanResponse();
            HunyuanResponse.ErrorInfo errorInfo = HunyuanResponse.ErrorInfo.builder()
                .code("InternalError")
                .message("Internal server error: " + e.getMessage())
                .build();
            
            HunyuanResponse.ResponseData responseData = HunyuanResponse.ResponseData.builder()
                .error(errorInfo)
                .build();
            errorResponse.setResponse(responseData);
            
            return errorResponse;
        }
    }
    
    @Override
    public void validateRequest(HunyuanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        // 检查必填参数：Prompt、ImageBase64、ImageUrl 三者必填其一
        boolean hasPrompt = request.getPrompt() != null && !request.getPrompt().trim().isEmpty();
        boolean hasImageBase64 = request.getImageBase64() != null && !request.getImageBase64().trim().isEmpty();
        boolean hasImageUrl = request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty();
        
        if (!hasPrompt && !hasImageBase64 && !hasImageUrl) {
            throw new IllegalArgumentException("提示词(Prompt)、图片Base64(ImageBase64)、图片URL(ImageUrl) 三者必填其一");
        }
        
        // Prompt和Image不能同时存在
        if (hasPrompt && (hasImageBase64 || hasImageUrl)) {
            throw new IllegalArgumentException("提示词(Prompt)和图片(ImageBase64/ImageUrl)不能同时存在");
        }
        
        // ImageBase64和ImageUrl不能同时存在
        if (hasImageBase64 && hasImageUrl) {
            throw new IllegalArgumentException("图片Base64(ImageBase64)和图片URL(ImageUrl)不能同时存在");
        }
        
        // 验证提示词长度
        if (hasPrompt && request.getPrompt().length() > 1024) {
            throw new IllegalArgumentException("提示词长度不能超过1024个字符");
        }
        
        // 验证结果格式
        if (request.getResultFormat() != null && !request.getResultFormat().trim().isEmpty()) {
            String format = request.getResultFormat().toUpperCase();
            if (!"OBJ".equals(format) && !"GLB".equals(format) && !"STL".equals(format) 
                && !"USDZ".equals(format) && !"FBX".equals(format) && !"MP4".equals(format)) {
                throw new IllegalArgumentException("不支持的结果格式，支持的格式: OBJ, GLB, STL, USDZ, FBX, MP4");
            }
        }
    }
    
    /**
     * 构建请求头，包含腾讯云签名
     */
    private HttpHeaders buildHeaders(String action, String payload) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String service = "ai3d";
        String version = "2025-05-13";
        String region = tencentCloudConfig.getRegion();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
        // 构建规范请求
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                                 "host:" + tencentCloudConfig.getHunyuan().getEndpoint() + "\n" +
                                 "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" +
                                canonicalUri + "\n" +
                                canonicalQueryString + "\n" +
                                canonicalHeaders + "\n" +
                                signedHeaders + "\n" +
                                hashedRequestPayload;
        
        // 构建待签名字符串
        String algorithm = "TC3-HMAC-SHA256";
        String credentialScope = date + "/" + service + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" +
                            timestamp + "\n" +
                            credentialScope + "\n" +
                            hashedCanonicalRequest;
        
        // 计算签名
        byte[] secretDate = hmacSha256(("TC3" + credential.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, service);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));
        
        // 构建Authorization
        String authorization = algorithm + " " +
                             "Credential=" + credential.getSecretId() + "/" + credentialScope + ", " +
                             "SignedHeaders=" + signedHeaders + ", " +
                             "Signature=" + signature;
        
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Host", tencentCloudConfig.getHunyuan().getEndpoint());
        headers.set("X-TC-Action", action);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Version", version);
        headers.set("X-TC-Region", region);
        
        return headers;
    }
    
    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }
    
    private byte[] hmacSha256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}