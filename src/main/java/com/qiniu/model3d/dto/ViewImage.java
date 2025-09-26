package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.NotBlank;

/**
 * 多视角图片模型
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViewImage {

    /**
     * 视角类型：left（左视图）、right（右视图）、back（后视图）
     */
    @NotBlank(message = "视角类型不能为空")
    private String view;

    /**
     * 图片Base64编码数据
     */
    private String imageBase64;

    /**
     * 图片URL
     */
    private String imageUrl;

    public ViewImage() {}

    public ViewImage(String view, String imageBase64) {
        this.view = view;
        this.imageBase64 = imageBase64;
    }

    public ViewImage(String view, String imageBase64, String imageUrl) {
        this.view = view;
        this.imageBase64 = imageBase64;
        this.imageUrl = imageUrl;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
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

    @Override
    public String toString() {
        return "ViewImage{" +
                "view='" + view + '\'' +
                ", imageBase64='" + (imageBase64 != null ? "[BASE64_DATA]" : null) + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}