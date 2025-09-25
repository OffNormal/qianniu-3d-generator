package org.example.infrastructure.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * API提供商工厂类 - 只保留腾讯混元API
 */
@Slf4j
@Component
public class ApiProviderFactory {
    
    // 注释掉Meshy相关的依赖
    // @Resource
    // private MeshyApiGateway meshyApiGateway;
    
    @Resource
    private HunyuanApiGateway hunyuanApiGateway;
    
    // 修改默认提供商为腾讯混元
    @Value("${api.provider.default:hunyuan}")
    private String defaultProvider;
    
    @Value("${api.provider.fallback:hunyuan}")
    private String fallbackProvider;
    
    @Value("${api.provider.enable-fallback:false}")
    private boolean enableFallback;
    
    /**
     * 文本转3D
     */
    public String textTo3D(String text, Map<String, Object> params) {
        return textTo3D(text, params, defaultProvider);
    }
    
    /**
     * 文本转3D（指定提供商）
     */
    public String textTo3D(String text, Map<String, Object> params, String provider) {
        try {
            switch (provider.toLowerCase()) {
                // 注释掉Meshy相关逻辑
                // case "meshy":
                //     return meshyApiGateway.textTo3D(text, params);
                case "hunyuan":
                    return hunyuanApiGateway.textTo3D(text, params);
                default:
                    throw new IllegalArgumentException("不支持的API提供商: " + provider + "，当前只支持腾讯混元(hunyuan)");
            }
        } catch (Exception e) {
            log.error("使用{}提供商进行文本转3D失败: {}", provider, e.getMessage());
            
            // 注释掉备用API逻辑，因为只有腾讯混元一个提供商
            /*
            // 如果启用了备用API且当前不是备用API，则尝试备用API
            if (enableFallback && !provider.equals(fallbackProvider)) {
                log.info("尝试使用备用API提供商: {}", fallbackProvider);
                try {
                    return textTo3D(text, params, fallbackProvider);
                } catch (Exception fallbackException) {
                    log.error("备用API提供商也失败: {}", fallbackException.getMessage());
                    throw new RuntimeException("所有API提供商都失败了", fallbackException);
                }
            }
            */
            
            throw e;
        }
    }
    
    /**
     * 图片转3D
     */
    public String imageTo3D(String imageUrl, Map<String, Object> params) {
        return imageTo3D(imageUrl, params, defaultProvider);
    }
    
    /**
     * 图片转3D（指定提供商）
     */
    public String imageTo3D(String imageUrl, Map<String, Object> params, String provider) {
        try {
            switch (provider.toLowerCase()) {
                // 注释掉Meshy相关逻辑
                // case "meshy":
                //     return meshyApiGateway.imageTo3D(imageUrl, params);
                case "hunyuan":
                    return hunyuanApiGateway.imageTo3D(imageUrl, params);
                default:
                    throw new IllegalArgumentException("不支持的API提供商: " + provider + "，当前只支持腾讯混元(hunyuan)");
            }
        } catch (Exception e) {
            log.error("使用{}提供商进行图片转3D失败: {}", provider, e.getMessage());
            
            // 注释掉备用API逻辑，因为只有腾讯混元一个提供商
            /*
            // 如果启用了备用API且当前不是备用API，则尝试备用API
            if (enableFallback && !provider.equals(fallbackProvider)) {
                log.info("尝试使用备用API提供商: {}", fallbackProvider);
                try {
                    return imageTo3D(imageUrl, params, fallbackProvider);
                } catch (Exception fallbackException) {
                    log.error("备用API提供商也失败: {}", fallbackException.getMessage());
                    throw new RuntimeException("所有API提供商都失败了", fallbackException);
                }
            }
            */
            
            throw e;
        }
    }
    
    /**
     * 查询任务状态
     */
    public Object queryTaskStatus(String taskId, String provider) {
        try {
            switch (provider.toLowerCase()) {
                // 注释掉Meshy相关逻辑
                // case "meshy":
                //     return meshyApiGateway.queryTaskStatus(taskId);
                case "hunyuan":
                    return hunyuanApiGateway.queryTaskStatus(taskId);
                default:
                    throw new IllegalArgumentException("不支持的API提供商: " + provider + "，当前只支持腾讯混元(hunyuan)");
            }
        } catch (Exception e) {
            log.error("查询任务状态失败，提供商: {}, 任务ID: {}", provider, taskId, e);
            throw e;
        }
    }
    
    /**
     * 获取可用的API提供商列表 - 只返回腾讯混元
     */
    public String[] getAvailableProviders() {
        return new String[]{"hunyuan"};
    }
    
    /**
     * 获取默认提供商
     */
    public String getDefaultProvider() {
        return defaultProvider;
    }
    
    /**
     * 获取备用提供商
     */
    public String getFallbackProvider() {
        return fallbackProvider;
    }
    
    /**
     * 是否启用备用API
     */
    public boolean isEnableFallback() {
        return enableFallback;
    }
    
    /**
     * 是否启用备用API（别名方法）
     */
    public boolean isFallbackEnabled() {
        return enableFallback;
    }
    
    /**
     * 根据文本生成3D模型
     */
    public String generateFromText(String text, String provider) {
        return textTo3D(text, null, provider);
    }
    
    /**
     * 根据图片生成3D模型
     */
    public String generateFromImage(String imageUrl, String provider) {
        return imageTo3D(imageUrl, null, provider);
    }
}