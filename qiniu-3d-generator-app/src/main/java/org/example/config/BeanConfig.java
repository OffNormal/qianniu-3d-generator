package org.example.config;


import org.example.domain.generation.repository.IGenerationRepository;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean配置类
 */
@Configuration
public class BeanConfig {



    @Bean
    @Primary
    public IGenerationRepository generationRepository() {
        return new IGenerationRepository() {
            private final Map<String, GenerationTaskEntity> tasks = new ConcurrentHashMap<>();

            @Override
            public void save(GenerationTaskEntity taskEntity) {
                tasks.put(taskEntity.getTaskId(), taskEntity);
            }

            @Override
            public void update(GenerationTaskEntity taskEntity) {
                tasks.put(taskEntity.getTaskId(), taskEntity);
            }

            @Override
            public GenerationTaskEntity findByTaskId(String taskId) {
                return tasks.get(taskId);
            }

            @Override
            public List<GenerationTaskEntity> findByUserId(String userId, int limit) {
                return tasks.values().stream()
                        .filter(task -> userId.equals(task.getUserId()))
                        .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                        .limit(limit)
                        .collect(Collectors.toList());
            }

            @Override
            public GenerationTaskEntity findByInputHash(String inputHash) {
                return tasks.values().stream()
                        .filter(task -> inputHash.equals(task.getInputHash()))
                        .findFirst()
                        .orElse(null);
            }

            @Override
            public List<GenerationTaskEntity> findSimilarCompletedTasks(String inputContent, double similarity) {
                return tasks.values().stream()
                        .filter(task -> task.getStatus().name().equals("COMPLETED"))
                        .filter(task -> calculateSimilarity(task.getInputContent(), inputContent) >= similarity)
                        .sorted((a, b) -> Double.compare(
                                calculateSimilarity(b.getInputContent(), inputContent),
                                calculateSimilarity(a.getInputContent(), inputContent)
                        ))
                        .collect(Collectors.toList());
            }

            private double calculateSimilarity(String text1, String text2) {
                if (text1 == null || text2 == null) return 0.0;
                if (text1.equals(text2)) return 1.0;
                
                // 使用最长公共子序列算法计算相似度
                int lcs = longestCommonSubsequence(text1, text2);
                int maxLength = Math.max(text1.length(), text2.length());
                return maxLength > 0 ? (double) lcs / maxLength : 0.0;
            }

            private int longestCommonSubsequence(String text1, String text2) {
                int m = text1.length();
                int n = text2.length();
                int[][] dp = new int[m + 1][n + 1];
                
                for (int i = 1; i <= m; i++) {
                    for (int j = 1; j <= n; j++) {
                        if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                            dp[i][j] = dp[i - 1][j - 1] + 1;
                        } else {
                            dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                        }
                    }
                }
                
                return dp[m][n];
            }
        };
    }
}