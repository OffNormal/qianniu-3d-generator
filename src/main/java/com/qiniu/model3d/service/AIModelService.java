package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelTask;

import java.util.function.Consumer;

/**
 * AI模型生成服务接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public interface AIModelService {

    /**
     * 根据文本生成3D模型
     * 
     * @param text 文本描述
     * @param complexity 复杂度
     * @param format 输出格式
     * @param progressCallback 进度回调函数
     * @return 生成的模型文件路径
     */
    String generateModelFromText(String text, 
                               ModelTask.Complexity complexity, 
                               ModelTask.OutputFormat format,
                               Consumer<Integer> progressCallback) throws Exception;

    /**
     * 根据图片生成3D模型
     * 
     * @param imagePath 图片文件路径
     * @param description 可选的文本描述
     * @param complexity 复杂度
     * @param format 输出格式
     * @param progressCallback 进度回调函数
     * @return 生成的模型文件路径
     */
    String generateModelFromImage(String imagePath,
                                String description,
                                ModelTask.Complexity complexity,
                                ModelTask.OutputFormat format,
                                Consumer<Integer> progressCallback) throws Exception;

    /**
     * 生成模型预览图
     * 
     * @param modelPath 模型文件路径
     * @return 预览图文件路径
     */
    String generatePreviewImage(String modelPath) throws Exception;

    /**
     * 检查服务是否可用
     * 
     * @return 服务状态
     */
    boolean isServiceAvailable();
}