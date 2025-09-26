package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 提交混元生3D任务请求
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitHunyuanTo3DJobRequest {

    /**
     * 文生3D，3D内容的描述，中文正向提示词
     * 最多支持1024个 utf-8 字符
     * 文生3D, image、image_url和 prompt必填其一，且prompt和image/image_url不能同时存在
     */
    @Size(max = 1024, message = "提示词最多支持1024个字符")
    private String prompt;

    /**
     * 输入图 Base64 数据
     * 大小：单边分辨率要求不小于128，不大于5000。大小不超过8m
     * 格式：jpg，png，jpeg，webp
     * ImageBase64、ImageUrl和 Prompt必填其一，且Prompt和ImageBase64/ImageUrl不能同时存在
     */
    private String imageBase64;

    /**
     * 输入图Url
     * 大小：单边分辨率要求不小于128，不大于5000。大小不超过8m
     * 格式：jpg，png，jpeg，webp
     * ImageBase64/ImageUrl和 Prompt必填其一，且Prompt和ImageBase64/ImageUrl不能同时存在
     */
    private String imageUrl;

    /**
     * 多视角的模型图片
     * 视角参考值：left（左视图）、right（右视图）、back（后视图）
     * 每个视角仅限制一张图片
     */
    private List<ViewImage> multiViewImages;

    /**
     * 生成模型的格式，仅限制生成一种格式
     * 生成模型文件组默认返回obj格式
     * 可选值：OBJ，GLB，STL，USDZ，FBX，MP4
     */
    private String resultFormat;

    /**
     * 是否开启 PBR材质生成，默认 false
     */
    private Boolean enablePBR;

    public SubmitHunyuanTo3DJobRequest() {}

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ViewImage> getMultiViewImages() {
        return multiViewImages;
    }

    public void setMultiViewImages(List<ViewImage> multiViewImages) {
        this.multiViewImages = multiViewImages;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    public Boolean getEnablePBR() {
        return enablePBR;
    }

    public void setEnablePBR(Boolean enablePBR) {
        this.enablePBR = enablePBR;
    }

    /**
     * 验证请求参数
     */
    public boolean isValid() {
        // prompt、imageBase64、imageUrl 必须有一个不为空
        boolean hasInput = (prompt != null && !prompt.trim().isEmpty()) ||
                          (imageBase64 != null && !imageBase64.trim().isEmpty()) ||
                          (imageUrl != null && !imageUrl.trim().isEmpty());
        
        if (!hasInput) {
            return false;
        }

        // prompt 和 image 不能同时存在
        boolean hasPrompt = prompt != null && !prompt.trim().isEmpty();
        boolean hasImage = (imageBase64 != null && !imageBase64.trim().isEmpty()) ||
                          (imageUrl != null && !imageUrl.trim().isEmpty());
        
        return !(hasPrompt && hasImage);
    }

    @Override
    public String toString() {
        return "SubmitHunyuanTo3DJobRequest{" +
                "prompt='" + prompt + '\'' +
                ", imageBase64='" + (imageBase64 != null ? "[BASE64_DATA]" : null) + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", multiViewImages=" + multiViewImages +
                ", resultFormat='" + resultFormat + '\'' +
                ", enablePBR=" + enablePBR +
                '}';
    }
}