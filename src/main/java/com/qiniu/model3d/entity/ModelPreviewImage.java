package com.qiniu.model3d.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型预览图片实体类
 * 支持一个模型对应多张预览图片
 */
@Entity
@Table(name = "model_preview_images")
public class ModelPreviewImage implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 图片文件路径
     */
    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    /**
     * 图片类型：PREVIEW(预览图), TEXTURE(纹理图), NORMAL(法线图)
     */
    @Column(name = "image_type", length = 20)
    private String imageType = "PREVIEW";

    /**
     * 图片显示顺序，从0开始
     */
    @Column(name = "image_order")
    private Integer imageOrder = 0;

    /**
     * 图片文件大小（字节）
     */
    @Column(name = "image_size")
    private Long imageSize;

    /**
     * 图片宽度（像素）
     */
    @Column(name = "image_width")
    private Integer imageWidth;

    /**
     * 图片高度（像素）
     */
    @Column(name = "image_height")
    private Integer imageHeight;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 关联的模型任务
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "task_id")
    private ModelTask modelTask;

    // 构造函数
    public ModelPreviewImage() {}

    public ModelPreviewImage(String imagePath, String imageType, Integer imageOrder) {
        this.imagePath = imagePath;
        this.imageType = imageType;
        this.imageOrder = imageOrder;
        this.createdAt = LocalDateTime.now();
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public Integer getImageOrder() {
        return imageOrder;
    }

    public void setImageOrder(Integer imageOrder) {
        this.imageOrder = imageOrder;
    }

    public Long getImageSize() {
        return imageSize;
    }

    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ModelTask getModelTask() {
        return modelTask;
    }

    public void setModelTask(ModelTask modelTask) {
        this.modelTask = modelTask;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 图片类型枚举
     */
    public enum ImageType {
        PREVIEW("PREVIEW"),
        TEXTURE("TEXTURE"),
        NORMAL("NORMAL");

        private final String value;

        ImageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}