package com.qiniu.model3d.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC配置类
 * 配置静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.model-dir:./models}")
    private String modelDir;

    @Value("${app.file.preview-dir:./previews}")
    private String previewDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        String absoluteUploadDir = new File(uploadDir).getAbsolutePath();
        String absoluteModelDir = new File(modelDir).getAbsolutePath();
        String absolutePreviewDir = new File(previewDir).getAbsolutePath();

        logger.info("配置静态资源映射:");
        logger.info("上传目录: {} -> {}", uploadDir, absoluteUploadDir);
        logger.info("模型目录: {} -> {}", modelDir, absoluteModelDir);
        logger.info("预览目录: {} -> {}", previewDir, absolutePreviewDir);

        // 配置上传文件访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadDir + "/");

        // 配置模型文件访问路径
        registry.addResourceHandler("/models/**")
                .addResourceLocations("file:" + absoluteModelDir + "/");

        // 配置预览图片访问路径
        registry.addResourceHandler("/previews/**")
                .addResourceLocations("file:" + absolutePreviewDir + "/");

        // 配置静态资源访问路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        logger.info("静态资源映射配置完成");
    }
}