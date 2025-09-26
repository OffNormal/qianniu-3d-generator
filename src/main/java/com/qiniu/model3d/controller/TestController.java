package com.qiniu.model3d.controller;

import com.qiniu.model3d.dto.ApiResponse;
import com.qiniu.model3d.service.AIModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于验证各种服务的集成状态
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin(origins = "*")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private AIModelService aiModelService;

    /**
     * 测试当前AI服务可用性（可能是腾讯混元或Mock服务）
     */
    @GetMapping("/ai-service/status")
    public ApiResponse<Map<String, Object>> testAIServiceStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (aiModelService != null) {
                boolean isAvailable = aiModelService.isServiceAvailable();
                String serviceName = aiModelService.getClass().getSimpleName();
                boolean isTencentService = serviceName.contains("TencentHunyuan");
                
                result.put("serviceExists", true);
                result.put("isAvailable", isAvailable);
                result.put("serviceName", serviceName);
                result.put("serviceType", isTencentService ? "tencent" : "mock");
                
                if (isAvailable) {
                    logger.info("AI服务状态检查: {} 可用", serviceName);
                    return ApiResponse.success("AI服务可用", result);
                } else {
                    logger.warn("AI服务状态检查: {} 不可用", serviceName);
                    return ApiResponse.<Map<String, Object>>error("AI服务不可用");
                }
            } else {
                result.put("serviceExists", false);
                result.put("isAvailable", false);
                result.put("message", "AI服务未注入");
                
                logger.warn("AI服务状态检查: 服务未注入");
                return ApiResponse.<Map<String, Object>>error("AI服务未配置");
            }
            
        } catch (Exception e) {
            logger.error("AI服务状态检查失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return ApiResponse.<Map<String, Object>>error("服务状态检查失败");
        }
    }

    /**
     * 测试默认AI服务可用性（与ai-service/status相同，保持向后兼容）
     */
    @GetMapping("/default-ai/status")
    public ApiResponse<Map<String, Object>> testDefaultAIStatus() {
        return testAIServiceStatus();
    }

    /**
     * 获取所有服务状态概览
     */
    @GetMapping("/services/overview")
    public ApiResponse<Map<String, Object>> getServicesOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 当前AI服务状态
            Map<String, Object> aiServiceStatus = new HashMap<>();
            if (aiModelService != null) {
                String serviceName = aiModelService.getClass().getSimpleName();
                boolean isTencentService = serviceName.contains("TencentHunyuan");
                
                aiServiceStatus.put("exists", true);
                aiServiceStatus.put("available", aiModelService.isServiceAvailable());
                aiServiceStatus.put("className", serviceName);
                aiServiceStatus.put("serviceType", isTencentService ? "tencent" : "mock");
            } else {
                aiServiceStatus.put("exists", false);
                aiServiceStatus.put("available", false);
                aiServiceStatus.put("className", null);
                aiServiceStatus.put("serviceType", "unknown");
            }
            overview.put("currentAIService", aiServiceStatus);
            
            // 为了向后兼容，保留原有的字段结构
            overview.put("defaultAI", aiServiceStatus);
            
            logger.info("服务状态概览查询完成");
            return ApiResponse.success("服务状态概览", overview);
            
        } catch (Exception e) {
            logger.error("获取服务状态概览失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return ApiResponse.<Map<String, Object>>error("获取服务状态概览失败");
        }
    }
}