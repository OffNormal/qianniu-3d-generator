package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.service.SimilarityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 相似度计算服务实现类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
public class SimilarityServiceImpl implements SimilarityService {

    // 权重配置
    @Value("${cache.similarity.semantic-weight:0.7}")
    private double semanticWeight;

    @Value("${cache.similarity.basic-weight:0.3}")
    private double basicWeight;

    // 阈值配置
    @Value("${cache.similarity.exact-threshold:1.0}")
    private double exactThreshold;

    @Value("${cache.similarity.high-threshold:0.8}")
    private double highThreshold;

    @Value("${cache.similarity.medium-threshold:0.6}")
    private double mediumThreshold;

    @Value("${cache.similarity.low-threshold:0.4}")
    private double lowThreshold;

    // 算法权重配置
    @Value("${cache.similarity.jaccard-weight:0.4}")
    private double jaccardWeight;

    @Value("${cache.similarity.cosine-weight:0.3}")
    private double cosineWeight;

    @Value("${cache.similarity.length-weight:0.2}")
    private double lengthWeight;

    @Value("${cache.similarity.ngram-weight:0.1}")
    private double ngramWeight;

    // 常量
    private static final Pattern WORD_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]");
    private static final int DEFAULT_NGRAM_SIZE = 2;

    @Override
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        if (text1.equals(text2)) {
            return 1.0;
        }

        // 组合语义相似度和基础相似度
        double semanticSim = calculateSemanticSimilarity(text1, text2);
        double basicSim = calculateBasicSimilarity(text1, text2);

        return semanticSim * semanticWeight + basicSim * basicWeight;
    }

    @Override
    public double calculateSemanticSimilarity(String text1, String text2) {
        // 这里是增强的语义相似度实现
        // 实际项目中可以集成更复杂的NLP模型，如BERT、Word2Vec等
        
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        // 标准化文本
        String norm1 = normalizeText(text1);
        String norm2 = normalizeText(text2);

        if (norm1.equals(norm2)) {
            return 1.0;
        }

        // 计算多种相似度指标
        double jaccardSim = calculateJaccardSimilarity(norm1, norm2);
        double cosineSim = calculateCosineSimilarity(norm1, norm2);
        double lengthSim = calculateLengthSimilarity(norm1, norm2);
        double ngramSim = calculateNGramSimilarity(norm1, norm2);
        
        // 加权组合多种算法
        return jaccardSim * jaccardWeight + 
               cosineSim * cosineWeight + 
               lengthSim * lengthWeight + 
               ngramSim * ngramWeight;
    }

    @Override
    public double calculateBasicSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        if (text1.equals(text2)) {
            return 1.0;
        }

        // 使用编辑距离计算基础相似度
        int editDistance = calculateEditDistance(text1, text2);
        int maxLength = Math.max(text1.length(), text2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - (double) editDistance / maxLength;
    }

    /**
     * 标准化文本
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.toLowerCase()
                  .replaceAll(NORMALIZE_PATTERN.pattern(), " ") // 保留字母、数字、中文和空格
                  .replaceAll("\\s+", " ") // 合并多个空格
                  .trim();
    }

    /**
     * 计算Jaccard相似度
     */
    private double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(WORD_PATTERN.split(text1)));
        Set<String> words2 = new HashSet<>(Arrays.asList(WORD_PATTERN.split(text2)));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(String text1, String text2) {
        Map<String, Integer> freq1 = getWordFrequency(text1);
        Map<String, Integer> freq2 = getWordFrequency(text2);

        Set<String> allWords = new HashSet<>();
        allWords.addAll(freq1.keySet());
        allWords.addAll(freq2.keySet());

        if (allWords.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String word : allWords) {
            int count1 = freq1.getOrDefault(word, 0);
            int count2 = freq2.getOrDefault(word, 0);

            dotProduct += count1 * count2;
            norm1 += count1 * count1;
            norm2 += count2 * count2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算N-gram相似度
     */
    private double calculateNGramSimilarity(String text1, String text2) {
        Set<String> ngrams1 = generateNGrams(text1, DEFAULT_NGRAM_SIZE);
        Set<String> ngrams2 = generateNGrams(text2, DEFAULT_NGRAM_SIZE);

        if (ngrams1.isEmpty() && ngrams2.isEmpty()) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 获取词频统计
     */
    private Map<String, Integer> getWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        String[] words = WORD_PATTERN.split(text);

        for (String word : words) {
            if (!word.isEmpty()) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }

        return frequency;
    }

    /**
     * 生成N-gram
     */
    private Set<String> generateNGrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        
        if (text.length() < n) {
            ngrams.add(text);
            return ngrams;
        }

        for (int i = 0; i <= text.length() - n; i++) {
            ngrams.add(text.substring(i, i + n));
        }

        return ngrams;
    }

    /**
     * 计算长度相似性
     */
    private double calculateLengthSimilarity(String text1, String text2) {
        int len1 = text1.length();
        int len2 = text2.length();
        
        if (len1 == 0 && len2 == 0) {
            return 1.0;
        }
        
        int maxLen = Math.max(len1, len2);
        int minLen = Math.min(len1, len2);
        
        return (double) minLen / maxLen;
    }

    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int calculateEditDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();

        // 创建动态规划表
        int[][] dp = new int[len1 + 1][len2 + 1];

        // 初始化边界条件
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        // 填充动态规划表
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    @Override
    public boolean isExactMatch(double similarity) {
        return similarity >= exactThreshold;
    }

    @Override
    public boolean isHighSimilarity(double similarity) {
        return similarity >= highThreshold;
    }

    @Override
    public boolean isMediumSimilarity(double similarity) {
        return similarity >= mediumThreshold && similarity < highThreshold;
    }

    @Override
    public boolean isLowSimilarity(double similarity) {
        return similarity >= lowThreshold && similarity < mediumThreshold;
    }

    @Override
    public String getSimilarityLevel(double similarity) {
        if (isExactMatch(similarity)) {
            return "EXACT";
        } else if (isHighSimilarity(similarity)) {
            return "HIGH";
        } else if (isMediumSimilarity(similarity)) {
            return "MEDIUM";
        } else if (isLowSimilarity(similarity)) {
            return "LOW";
        } else {
            return "NONE";
        }
    }

    @Override
    public double calculateTextSimilarity(String text1, String text2) {
        // 对于文本相似度，使用现有的calculateSimilarity方法
        return calculateSimilarity(text1, text2);
    }

    @Override
    public double calculateImageSimilarity(String imagePath1, String imagePath2) {
        // 对于图片相似度，这里提供一个基础实现
        // 实际项目中可以集成图像相似度算法，如感知哈希、特征提取等
        if (imagePath1 == null || imagePath2 == null) {
            return 0.0;
        }
        
        if (imagePath1.equals(imagePath2)) {
            return 1.0;
        }
        
        // 基于文件名的简单相似度计算
        // 实际应用中应该使用图像处理算法
        String fileName1 = extractFileName(imagePath1);
        String fileName2 = extractFileName(imagePath2);
        
        return calculateSimilarity(fileName1, fileName2);
    }

    /**
     * 从文件路径中提取文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null) {
            return "";
        }
        
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }
        
        return filePath;
    }
}