package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 3D文件结果模型
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class File3D {

    /**
     * 文件类型：OBJ、GLB、STL、USDZ、FBX、MP4
     */
    private String type;

    /**
     * 3D文件下载URL
     */
    private String url;

    /**
     * 预览图片URL
     */
    private String previewImageUrl;

    public File3D() {}

    public File3D(String type, String url) {
        this.type = type;
        this.url = url;
    }

    public File3D(String type, String url, String previewImageUrl) {
        this.type = type;
        this.url = url;
        this.previewImageUrl = previewImageUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    @Override
    public String toString() {
        return "File3D{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", previewImageUrl='" + previewImageUrl + '\'' +
                '}';
    }
}