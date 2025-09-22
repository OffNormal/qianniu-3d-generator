package org.example.domain.generation.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.types.enums.GenerationType;

/**
 * 3D模型生成请求值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequest {
    
    /** 用户ID */
    private String userId;
    
    /** 生成类型 */
    private GenerationType generationType;
    
    /** 输入内容 */
    private String inputContent;
    
    /** 生成参数 */
    private GenerationParams params;
    
    /**
     * 生成参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationParams {
        
        /** 模型质量等级 (low/medium/high) */
        private String quality = "medium";
        
        /** 面数限制 */
        private Integer maxFaces = 10000;
        
        /** 纹理分辨率 */
        private Integer textureResolution = 1024;
        
        /** 是否生成纹理 */
        private Boolean generateTexture = true;
        
        /** 输出格式 */
        private String outputFormat = "glb";
        
        /** 风格参数 */
        private String style;
        
        /** 额外提示词 */
        private String additionalPrompt;
    }
}