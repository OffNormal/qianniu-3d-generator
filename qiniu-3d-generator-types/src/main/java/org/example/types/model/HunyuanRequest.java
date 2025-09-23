package org.example.types.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 腾讯混元3D API请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HunyuanRequest {
    
    /**
     * 文生3D，3D内容的描述，中文正向提示词
     * 最多支持1024个 utf-8 字符
     * 文生3D, image、image_url和 prompt必填其一，且prompt和image/image_url不能同时存在
     */
    @JsonProperty("Prompt")
    private String prompt;
    
    /**
     * 输入图 Base64 数据
     * 大小：单边分辨率要求不小于128，不大于5000。大小不超过8m
     * 格式：jpg，png，jpeg，webp
     * ImageBase64、ImageUrl和 Prompt必填其一，且Prompt和ImageBase64/ImageUrl不能同时存在
     */
    @JsonProperty("ImageBase64")
    private String imageBase64;
    
    /**
     * 输入图Url
     * 大小：单边分辨率要求不小于128，不大于5000。大小不超过8m
     * 格式：jpg，png，jpeg，webp
     * ImageBase64/ImageUrl和 Prompt必填其一，且Prompt和ImageBase64/ImageUrl不能同时存在
     */
    @JsonProperty("ImageUrl")
    private String imageUrl;
    
    /**
     * 多视角的模型图片
     * 视角参考值：left：左视图；right：右视图；back：后视图
     * 每个视角仅限制一张图片
     */
    @JsonProperty("MultiViewImages")
    private List<ViewImage> multiViewImages;
    
    /**
     * 生成模型的格式，仅限制生成一种格式
     * 生成模型文件组默认返回obj格式
     * 可选值：OBJ，GLB，STL，USDZ，FBX，MP4
     */
    @JsonProperty("ResultFormat")
    private String resultFormat;
    
    /**
     * 是否开启 PBR材质生成，默认 false
     */
    @JsonProperty("EnablePBR")
    private Boolean enablePBR;
    
    /**
     * 多视角图片模型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewImage {
        /**
         * 视角类型：left, right, back
         */
        @JsonProperty("View")
        private String view;
        
        /**
         * 图片Base64数据
         */
        @JsonProperty("ImageBase64")
        private String imageBase64;
        
        /**
         * 图片URL
         */
        @JsonProperty("ImageUrl")
        private String imageUrl;
    }
}