package org.example.types.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 腾讯混元3D API响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HunyuanResponse {
    
    /**
     * 响应数据
     */
    @JsonProperty("Response")
    private ResponseData response;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseData {
        /**
         * 任务ID（有效期24小时）
         */
        @JsonProperty("JobId")
        private String jobId;
        
        /**
         * 唯一请求 ID，由服务端生成，每次请求都会返回
         */
        @JsonProperty("RequestId")
        private String requestId;
        
        /**
         * 任务状态
         */
        @JsonProperty("Status")
        private String status;
        
        /**
         * 结果文件URL
         */
        @JsonProperty("ResultUrl")
        private String resultUrl;
        
        /**
         * 错误信息（如果有）
         */
        @JsonProperty("Error")
        private ErrorInfo error;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        /**
         * 错误码
         */
        @JsonProperty("Code")
        private String code;
        
        /**
         * 错误信息
         */
        @JsonProperty("Message")
        private String message;
    }
}