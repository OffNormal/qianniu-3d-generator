package org.example.infrastructure.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.config.TencentCloudConfig;
import org.example.domain.generation.service.IHunyuanService;
import org.example.types.model.HunyuanRequest;
import org.example.types.model.HunyuanResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯混元3D服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HunyuanServiceImpl implements IHunyuanService {
    
    private final TencentCloudConfig tencentCloudConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String SERVICE = "ai3d";
    private static final String ACTION = "SubmitHunyuanTo3DJob";
    
    @Override
    public HunyuanResponse submitHunyuanTo3DJob(HunyuanRequest request) {
        try {
            // 验证请求参数
            validateRequest(request);
            
            // 构建请求
            String url = "https://" + tencentCloudConfig.getHunyuan().getEndpoint();
            HttpHeaders headers = buildHeaders(request);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
            
            log.info("提交腾讯混元3D任务，请求参数: {}", objectMapper.writeValueAsString(request));
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.info("腾讯混元3D任务提交响应: {}", response.getBody());
            
            // 解析响应
            return objectMapper.readValue(response.getBody(), HunyuanResponse.class);
            
        } catch (Exception e) {
            log.error("提交腾讯混元3D任务失败", e);
            throw new RuntimeException("提交腾讯混元3D任务失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Object queryJobStatus(String jobId) {
        try {
            // 构建查询请求
            String url = "https://" + tencentCloudConfig.getHunyuan().getEndpoint();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("JobId", jobId);
            
            HttpHeaders headers = buildQueryHeaders(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            
            log.info("查询腾讯混元3D任务状态，任务ID: {}", jobId);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.info("腾讯混元3D任务状态查询响应: {}", response.getBody());
            
            // 解析响应
            return objectMapper.readValue(response.getBody(), Object.class);
            
        } catch (Exception e) {
            log.error("查询腾讯混元3D任务状态失败", e);
            throw new RuntimeException("查询腾讯混元3D任务状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateRequest(HunyuanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new IllegalArgumentException("提示词不能为空");
        }
        
        if (request.getPrompt().length() > 1000) {
            throw new IllegalArgumentException("提示词长度不能超过1000个字符");
        }
    }
    
    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(HunyuanRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 构建腾讯云API签名
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
        // 构建规范请求字符串
        String canonicalRequest = buildCanonicalRequest(request);
        
        // 构建待签名字符串
        String stringToSign = buildStringToSign(timestamp, date, canonicalRequest);
        
        // 计算签名
        String signature = calculateSignature(date, stringToSign);
        
        // 构建Authorization头
        String authorization = buildAuthorization(timestamp, date, signature);
        
        headers.set("Authorization", authorization);
        headers.set("X-TC-Action", ACTION);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Version", "2022-07-01");
        headers.set("X-TC-Region", tencentCloudConfig.getRegion());
        
        return headers;
    }
    
    /**
     * 构建查询请求头
     */
    private HttpHeaders buildQueryHeaders(Map<String, Object> requestBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 构建腾讯云API签名
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
        // 构建规范请求字符串
        String canonicalRequest = buildCanonicalRequestForQuery(requestBody);
        
        // 构建待签名字符串
        String stringToSign = buildStringToSign(timestamp, date, canonicalRequest);
        
        // 计算签名
        String signature = calculateSignature(date, stringToSign);
        
        // 构建Authorization头
        String authorization = buildAuthorization(timestamp, date, signature);
        
        headers.set("Authorization", authorization);
        headers.set("X-TC-Action", "DescribeHunyuanTo3DJob");
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Version", "2022-07-01");
        headers.set("X-TC-Region", tencentCloudConfig.getRegion());
        
        return headers;
    }
    
    private String buildCanonicalRequest(HunyuanRequest request) throws Exception {
        String httpMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                                "host:" + tencentCloudConfig.getHunyuan().getEndpoint() + "\n";
        String signedHeaders = "content-type;host";
        String payload = objectMapper.writeValueAsString(request);
        String hashedPayload = sha256Hex(payload);
        
        return httpMethod + "\n" +
               canonicalUri + "\n" +
               canonicalQueryString + "\n" +
               canonicalHeaders + "\n" +
               signedHeaders + "\n" +
               hashedPayload;
    }
    
    private String buildCanonicalRequestForQuery(Map<String, Object> requestBody) throws Exception {
        String httpMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                                "host:" + tencentCloudConfig.getHunyuan().getEndpoint() + "\n";
        String signedHeaders = "content-type;host";
        String payload = objectMapper.writeValueAsString(requestBody);
        String hashedPayload = sha256Hex(payload);
        
        return httpMethod + "\n" +
               canonicalUri + "\n" +
               canonicalQueryString + "\n" +
               canonicalHeaders + "\n" +
               signedHeaders + "\n" +
               hashedPayload;
    }
    
    private String buildStringToSign(String timestamp, String date, String canonicalRequest) throws Exception {
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        
        return ALGORITHM + "\n" +
               timestamp + "\n" +
               credentialScope + "\n" +
               hashedCanonicalRequest;
    }
    
    private String calculateSignature(String date, String stringToSign) throws Exception {
        byte[] secretDate = hmacSha256(("TC3" + tencentCloudConfig.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        return bytesToHex(hmacSha256(secretSigning, stringToSign));
    }
    
    private String buildAuthorization(String timestamp, String date, String signature) {
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String credential = tencentCloudConfig.getSecretId() + "/" + credentialScope;
        
        return ALGORITHM + " " +
               "Credential=" + credential + ", " +
               "SignedHeaders=content-type;host, " +
               "Signature=" + signature;
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