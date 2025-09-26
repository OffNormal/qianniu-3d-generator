package com.qiniu.model3d.dto;

import com.qiniu.model3d.entity.ModelTask;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 文本生成3D模型请求DTO
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public class TextGenerationRequest {

    @NotBlank(message = "文本描述不能为空")
    @Size(min = 10, max = 500, message = "文本长度必须在10-500字符之间")
    private String text;

    private ModelTask.Complexity complexity = ModelTask.Complexity.SIMPLE;

    private ModelTask.OutputFormat format = ModelTask.OutputFormat.OBJ;

    private GenerationOptions options;

    // 构造函数
    public TextGenerationRequest() {}

    public TextGenerationRequest(String text) {
        this.text = text;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ModelTask.Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(ModelTask.Complexity complexity) {
        this.complexity = complexity;
    }

    public ModelTask.OutputFormat getFormat() {
        return format;
    }

    public void setFormat(ModelTask.OutputFormat format) {
        this.format = format;
    }

    public GenerationOptions getOptions() {
        return options;
    }

    public void setOptions(GenerationOptions options) {
        this.options = options;
    }

    /**
     * 生成选项内部类
     */
    public static class GenerationOptions {
        private String color = "#CCCCCC";
        private String size = "medium";
        private String style = "realistic";

        // Getters and Setters
        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    @Override
    public String toString() {
        return "TextGenerationRequest{" +
                "text='" + text + '\'' +
                ", complexity=" + complexity +
                ", format=" + format +
                ", options=" + options +
                '}';
    }
}