package org.example.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudConfig {
    
    /** 腾讯云访问密钥ID */
    private String secretId;
    
    /** 腾讯云访问密钥 */
    private String secretKey;
    
    /** 地域 */
    private String region = "ap-beijing";
    
    /** 混元3D配置 */
    private HunyuanConfig hunyuan = new HunyuanConfig();
    
    @Data
    public static class HunyuanConfig {
        /** 服务端点 */
        private String endpoint = "ai3d.tencentcloudapi.com";
        
        /** API版本 */
        private String version = "2023-05-08";
        
        /** 超时时间（毫秒） */
        private int timeout = 30000;
    }
}