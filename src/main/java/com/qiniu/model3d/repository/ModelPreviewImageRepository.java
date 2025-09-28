package com.qiniu.model3d.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qiniu.model3d.entity.ModelPreviewImage;

/**
 * 模型预览图片Repository接口
 */
@Repository
public interface ModelPreviewImageRepository extends JpaRepository<ModelPreviewImage, Long> {

    /**
     * 根据任务ID查找所有预览图片，按顺序排序
     */
    @Query("SELECT p FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId ORDER BY p.imageOrder")
    List<ModelPreviewImage> findByTaskIdOrderByImageOrder(@Param("taskId") String taskId);

    /**
     * 根据任务ID和图片类型查找预览图片，按顺序排序
     */
    @Query("SELECT p FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId AND p.imageType = :imageType ORDER BY p.imageOrder")
    List<ModelPreviewImage> findByTaskIdAndImageTypeOrderByImageOrder(@Param("taskId") String taskId, @Param("imageType") String imageType);

    /**
     * 根据任务ID删除所有预览图片
     */
    @Query("DELETE FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID统计预览图片数量
     */
    @Query("SELECT COUNT(p) FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId")
    long countByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID和图片类型统计预览图片数量
     */
    @Query("SELECT COUNT(p) FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId AND p.imageType = :imageType")
    long countByTaskIdAndImageType(@Param("taskId") String taskId, @Param("imageType") String imageType);

    /**
     * 查找指定任务的第一张预览图片
     */
    @Query("SELECT p FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId AND p.imageType = 'PREVIEW' ORDER BY p.imageOrder ASC")
    ModelPreviewImage findFirstPreviewImageByTaskId(@Param("taskId") String taskId);

    /**
     * 批量查询多个任务的预览图片
     */
    @Query("SELECT p FROM ModelPreviewImage p WHERE p.modelTask.taskId IN :taskIds ORDER BY p.modelTask.taskId, p.imageOrder")
    List<ModelPreviewImage> findByTaskIdInOrderByTaskIdAndImageOrder(@Param("taskIds") List<String> taskIds);

    /**
     * 查找指定任务的所有预览类型图片
     */
    @Query("SELECT p FROM ModelPreviewImage p WHERE p.modelTask.taskId = :taskId AND p.imageType = 'PREVIEW' ORDER BY p.imageOrder")
    List<ModelPreviewImage> findPreviewImagesByTaskId(@Param("taskId") String taskId);
}