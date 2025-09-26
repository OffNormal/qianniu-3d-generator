package com.qiniu.model3d.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云配置类
 * 用于管理腾讯云相关的配置参数
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudConfig {

    /**
     * 腾讯云 Secret ID
     */
    private String secretId;

    /**
     * 腾讯云 Secret Key
     */
    private String secretKey;

    /**
     * 腾讯云地域
     */
    private String region;

    /**
     * AI3D服务配置
     */
    private AI3DConfig ai3d = new AI3DConfig();

    // Getters and Setters
    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public AI3DConfig getAi3d() {
        return ai3d;
    }

    public void setAi3d(AI3DConfig ai3d) {
        this.ai3d = ai3d;
    }

    /**
     * AI3D服务配置内部类
     */
    public static class AI3DConfig {
        /**
         * 服务端点
         */
        private String endpoint = "hunyuan.tencentcloudapi.com";

        /**
         * API版本
         */
        private String version = "2023-09-01";

        /**
         * 请求超时时间（毫秒）
         */
        private int timeout = 30000;

        /**
         * 最大重试次数
         */
        private int retryCount = 60;

        /**
         * 生成配置
         */
        private GenerationConfig generation = new GenerationConfig();

        // Getters and Setters
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public GenerationConfig getGeneration() {
            return generation;
        }

        public void setGeneration(GenerationConfig generation) {
            this.generation = generation;
        }
    }

    /**
     * 生成配置内部类
     */
    public static class GenerationConfig {
        /**
         * 复杂度
         */
        private String complexity = "medium";

        /**
         * 输出格式
         */
        private String format = "jpg";

        /**
         * 风格
         */
        private String style = "201";

        // Getters and Setters
        public String getComplexity() {
            return complexity;
        }

        public void setComplexity(String complexity) {
            this.complexity = complexity;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }
}