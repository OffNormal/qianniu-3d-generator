package com.qiniu.model3d.service;

/**
 * 相似度计算服务接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public interface SimilarityService {

    /**
     * 计算两个文本的相似度
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数 (0.0 - 1.0)
     */
    double calculateSimilarity(String text1, String text2);

    /**
     * 计算语义相似度
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 语义相似度分数 (0.0 - 1.0)
     */
    double calculateSemanticSimilarity(String text1, String text2);

    /**
     * 计算基础相似度（基于字符串匹配）
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 基础相似度分数 (0.0 - 1.0)
     */
    double calculateBasicSimilarity(String text1, String text2);

    /**
     * 判断是否为精确匹配
     * 
     * @param similarity 相似度值
     * @return 是否为精确匹配
     */
    boolean isExactMatch(double similarity);

    /**
     * 判断是否为高相似度匹配
     * 
     * @param similarity 相似度值
     * @return 是否为高相似度匹配
     */
    boolean isHighSimilarity(double similarity);

    /**
     * 判断是否为中等相似度匹配
     * 
     * @param similarity 相似度值
     * @return 是否为中等相似度匹配
     */
    boolean isMediumSimilarity(double similarity);

    /**
     * 判断是否为低相似度匹配
     * 
     * @param similarity 相似度值
     * @return 是否为低相似度匹配
     */
    boolean isLowSimilarity(double similarity);

    /**
     * 获取相似度等级
     * 
     * @param similarity 相似度值
     * @return 相似度等级字符串
     */
    String getSimilarityLevel(double similarity);

    /**
     * 计算两个文本的相似度（专用于文本任务）
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数 (0.0 - 1.0)
     */
    double calculateTextSimilarity(String text1, String text2);

    /**
     * 计算两个图片的相似度（专用于图片任务）
     * 
     * @param imagePath1 图片路径1
     * @param imagePath2 图片路径2
     * @return 相似度分数 (0.0 - 1.0)
     */
    double calculateImageSimilarity(String imagePath1, String imagePath2);
}