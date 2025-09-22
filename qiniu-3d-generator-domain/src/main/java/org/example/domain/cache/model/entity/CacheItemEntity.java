package org.example.domain.cache.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.types.enums.GenerationType;

import java.time.LocalDateTime;

/**
 * 缓存项实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheItemEntity {
    
    /** 缓存键 */
    private String cacheKey;
    
    /** 输入内容 */
    private String inputContent;
    
    /** 输入哈希 */
    private String inputHash;
    
    /** 生成类型 */
    private GenerationType generationType;
    
    /** 结果URL */
    private String resultUrl;
    
    /** 模型文件路径 */
    private String modelFilePath;
    
    /** 预览图URL */
    private String previewImageUrl;
    
    /** 质量评分 */
    private Double qualityScore;
    
    /** 命中次数 */
    private Integer hitCount;
    
    /** 最后命中时间 */
    private LocalDateTime lastHitTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 过期时间 */
    private LocalDateTime expireTime;
    
    /**
     * 命中缓存
     */
    public void hit() {
        this.hitCount = (this.hitCount == null ? 0 : this.hitCount) + 1;
        this.lastHitTime = LocalDateTime.now();
    }
    
    /**
     * 是否过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 是否热门缓存（命中次数大于阈值）
     */
    public boolean isHot(int threshold) {
        return hitCount != null && hitCount >= threshold;
    }
}