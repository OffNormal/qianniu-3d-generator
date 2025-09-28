package com.qiniu.model3d.service;

import com.qiniu.model3d.entity.ModelPreviewImage;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelPreviewImageRepository;
import com.qiniu.model3d.repository.ModelTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 模型预览图片服务类
 */
@Service
@Transactional
public class ModelPreviewImageService {

    private static final Logger logger = LoggerFactory.getLogger(ModelPreviewImageService.class);

    @Autowired
    private ModelPreviewImageRepository previewImageRepository;
    
    @Autowired
    private ModelTaskRepository modelTaskRepository;

    /**
     * 为任务保存多张预览图片
     */
    public void savePreviewImages(String taskId, List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            logger.warn("No preview images to save for task: {}", taskId);
            return;
        }

        // 获取ModelTask对象
        Optional<ModelTask> modelTaskOpt = modelTaskRepository.findByTaskId(taskId);
        if (modelTaskOpt.isEmpty()) {
            logger.error("ModelTask not found for taskId: {}", taskId);
            return;
        }
        
        ModelTask modelTask = modelTaskOpt.get();

        // 删除现有的预览图片记录
        deletePreviewImagesByTaskId(taskId);

        // 保存新的预览图片
        IntStream.range(0, imagePaths.size())
                .forEach(index -> {
                    String imagePath = imagePaths.get(index);
                    ModelPreviewImage previewImage = new ModelPreviewImage();
                    previewImage.setModelTask(modelTask);
                    previewImage.setImagePath(imagePath);
                    previewImage.setImageType(ModelPreviewImage.ImageType.PREVIEW.getValue());
                    previewImage.setImageOrder(index);
                    previewImage.setCreatedAt(LocalDateTime.now());

                    // 尝试获取图片文件信息
                    try {
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            previewImage.setImageSize(imageFile.length());
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to get image file info for: {}", imagePath, e);
                    }

                    previewImageRepository.save(previewImage);
                    logger.info("Saved preview image {} for task {}: {}", index, taskId, imagePath);
                });
    }

    /**
     * 为任务保存单张预览图片
     */
    public void savePreviewImage(String taskId, String imagePath, String imageType, int imageOrder) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            logger.warn("No preview image path provided for task: {}", taskId);
            return;
        }

        // 获取ModelTask对象
        Optional<ModelTask> modelTaskOpt = modelTaskRepository.findByTaskId(taskId);
        if (modelTaskOpt.isEmpty()) {
            logger.error("ModelTask not found for taskId: {}", taskId);
            return;
        }
        
        ModelTask modelTask = modelTaskOpt.get();

        ModelPreviewImage previewImage = new ModelPreviewImage();
        previewImage.setModelTask(modelTask);
        previewImage.setImagePath(imagePath);
        previewImage.setImageType(imageType);
        previewImage.setImageOrder(imageOrder);
        previewImage.setCreatedAt(LocalDateTime.now());

        // 尝试获取图片文件信息
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                previewImage.setImageSize(imageFile.length());
            }
        } catch (Exception e) {
            logger.warn("Failed to get image file info for: {}", imagePath, e);
        }

        previewImageRepository.save(previewImage);
        logger.info("Saved preview image for task {}: {}", taskId, imagePath);
    }

    /**
     * 获取任务的所有预览图片
     */
    public List<ModelPreviewImage> getPreviewImagesByTaskId(String taskId) {
        return previewImageRepository.findPreviewImagesByTaskId(taskId);
    }

    /**
     * 获取任务的第一张预览图片
     */
    public ModelPreviewImage getFirstPreviewImage(String taskId) {
        return previewImageRepository.findFirstPreviewImageByTaskId(taskId);
    }

    /**
     * 删除任务的所有预览图片
     */
    public void deletePreviewImagesByTaskId(String taskId) {
        try {
            previewImageRepository.deleteByTaskId(taskId);
            logger.info("Deleted all preview images for task: {}", taskId);
        } catch (Exception e) {
            logger.error("Failed to delete preview images for task: {}", taskId, e);
        }
    }

    /**
     * 批量获取多个任务的预览图片
     */
    public List<ModelPreviewImage> getPreviewImagesByTaskIds(List<String> taskIds) {
        return previewImageRepository.findByTaskIdInOrderByTaskIdAndImageOrder(taskIds);
    }

    /**
     * 统计任务的预览图片数量
     */
    public long countPreviewImages(String taskId) {
        return previewImageRepository.countByTaskIdAndImageType(taskId, ModelPreviewImage.ImageType.PREVIEW.getValue());
    }

    /**
     * 更新预览图片的顺序
     */
    public void updateImageOrder(String taskId, List<String> orderedImagePaths) {
        List<ModelPreviewImage> existingImages = getPreviewImagesByTaskId(taskId);
        
        for (int i = 0; i < orderedImagePaths.size(); i++) {
            String imagePath = orderedImagePaths.get(i);
            final int imageOrder = i; // 创建 effectively final 变量
            existingImages.stream()
                    .filter(img -> img.getImagePath().equals(imagePath))
                    .findFirst()
                    .ifPresent(img -> {
                        img.setImageOrder(imageOrder);
                        img.setUpdatedAt(LocalDateTime.now());
                        previewImageRepository.save(img);
                    });
        }
        
        logger.info("Updated image order for task: {}", taskId);
    }

    /**
     * 添加单张预览图片
     */
    public ModelPreviewImage addPreviewImage(String taskId, String imagePath, String imageType) {
        // 获取ModelTask对象
        Optional<ModelTask> modelTaskOpt = modelTaskRepository.findByTaskId(taskId);
        if (modelTaskOpt.isEmpty()) {
            logger.error("ModelTask not found for taskId: {}", taskId);
            return null;
        }
        
        ModelTask modelTask = modelTaskOpt.get();

        // 获取当前最大的顺序号
        List<ModelPreviewImage> existingImages = getPreviewImagesByTaskId(taskId);
        int nextOrder = existingImages.stream()
                .mapToInt(ModelPreviewImage::getImageOrder)
                .max()
                .orElse(-1) + 1;

        ModelPreviewImage previewImage = new ModelPreviewImage();
        previewImage.setModelTask(modelTask);
        previewImage.setImagePath(imagePath);
        previewImage.setImageType(imageType != null ? imageType : ModelPreviewImage.ImageType.PREVIEW.getValue());
        previewImage.setImageOrder(nextOrder);
        previewImage.setCreatedAt(LocalDateTime.now());

        // 尝试获取图片文件信息
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                previewImage.setImageSize(imageFile.length());
            }
        } catch (Exception e) {
            logger.warn("Failed to get image file info for: {}", imagePath, e);
        }

        ModelPreviewImage saved = previewImageRepository.save(previewImage);
        logger.info("Added preview image for task {}: {}", taskId, imagePath);
        return saved;
    }

    /**
     * 根据任务ID和图片ID获取图片路径
     */
    public String getPreviewImagePath(String taskId, Long imageId) {
        try {
            ModelPreviewImage previewImage = previewImageRepository.findById(imageId).orElse(null);
            if (previewImage != null && previewImage.getModelTask() != null && 
                taskId.equals(previewImage.getModelTask().getTaskId())) {
                return previewImage.getImagePath();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to get preview image path for task {} and image {}: {}", 
                        taskId, imageId, e.getMessage(), e);
            return null;
        }
    }
}