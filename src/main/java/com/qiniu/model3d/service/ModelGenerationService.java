package com.qiniu.model3d.service;

import com.qiniu.model3d.dto.TextGenerationRequest;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.ModelTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 3D模型生成服务
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
public class ModelGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ModelGenerationService.class);

    @Autowired
    private ModelTaskRepository modelTaskRepository;

    @Autowired
    private AIModelService aiModelService;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.model-dir}")
    private String modelDir;

    @Value("${app.file.preview-dir}")
    private String previewDir;

    @Value("${app.file.max-file-size}")
    private long maxFileSize;

    @Value("${app.ai.service-type:default}")
    private String aiServiceType;

    // 支持的图片格式
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    /**
     * 根据文本生成3D模型
     */
    public ModelTask generateFromText(TextGenerationRequest request, String clientIp) {
        // 验证输入
        validateTextRequest(request);
        
        // 创建任务
        ModelTask task = new ModelTask();
        task.setTaskId(generateTaskId());
        task.setType(ModelTask.TaskType.TEXT);
        task.setInputText(request.getText());
        task.setComplexity(request.getComplexity());
        task.setOutputFormat(request.getFormat());
        task.setStatus(ModelTask.TaskStatus.PENDING);
        task.setProgress(0);
        task.setClientIp(clientIp);
        task.setCreatedAt(LocalDateTime.now());
        
        // 保存任务
        task = modelTaskRepository.save(task);
        
        // 异步处理生成任务
        processTextGenerationAsync(task);
        
        logger.info("文本生成任务已创建: taskId={}, text={}", task.getTaskId(), request.getText());
        return task;
    }

    /**
     * 根据图片生成3D模型
     */
    public ModelTask generateFromImage(MultipartFile image, ModelTask.Complexity complexity, 
                                     ModelTask.OutputFormat format, String description, String clientIp) {
        // 验证图片
        validateImageFile(image);
        
        // 保存上传的图片
        String imagePath = saveUploadedImage(image);
        
        // 创建任务
        ModelTask task = new ModelTask();
        task.setTaskId(generateTaskId());
        task.setType(ModelTask.TaskType.IMAGE);
        task.setInputImagePath(imagePath);
        task.setInputText(description);
        task.setComplexity(complexity);
        task.setOutputFormat(format);
        task.setStatus(ModelTask.TaskStatus.PENDING);
        task.setProgress(0);
        task.setClientIp(clientIp);
        task.setCreatedAt(LocalDateTime.now());
        
        // 保存任务
        task = modelTaskRepository.save(task);
        
        // 异步处理生成任务
        processImageGenerationAsync(task);
        
        logger.info("图片生成任务已创建: taskId={}, imagePath={}", task.getTaskId(), imagePath);
        return task;
    }

    /**
     * 获取任务状态
     */
    public ModelTask getTaskStatus(String taskId) {
        return modelTaskRepository.findByTaskId(taskId).orElse(null);
    }

    /**
     * 获取用户的生成历史记录
     */
    public org.springframework.data.domain.Page<ModelTask> getGenerationHistory(String clientIp, int page, int size, ModelTask.TaskStatus status) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        
        if (status != null) {
            return modelTaskRepository.findByClientIpAndStatusOrderByCreatedAtDesc(clientIp, status, pageable);
        } else {
            return modelTaskRepository.findByClientIpOrderByCreatedAtDesc(clientIp, pageable);
        }
    }

    /**
     * 下载模型文件
     */
    public Resource downloadModel(String modelId, String format) {
        ModelTask task = modelTaskRepository.findByTaskId(modelId).orElse(null);
        if (task == null || task.getStatus() != ModelTask.TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("模型不存在或未完成生成");
        }
        
        String filePath = task.getModelFilePath();
        if (format != null && !format.isEmpty()) {
            // 如果指定了格式，尝试查找对应格式的文件
            String formatFilePath = getModelFilePathByFormat(task, format);
            if (formatFilePath != null) {
                filePath = formatFilePath;
            }
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("模型文件不存在");
        }
        
        return new FileSystemResource(file);
    }

    /**
     * 获取模型预览图
     */
    public Resource getModelPreview(String modelId, String angle, String size) {
        ModelTask task = modelTaskRepository.findByTaskId(modelId).orElse(null);
        if (task == null || task.getStatus() != ModelTask.TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("模型不存在或未完成生成");
        }
        
        String previewPath = task.getPreviewImagePath();
        if (previewPath == null || previewPath.isEmpty()) {
            throw new IllegalArgumentException("预览图不存在");
        }
        
        // 根据角度和大小调整预览图路径
        String adjustedPath = adjustPreviewPath(previewPath, angle, size);
        
        File file = new File(adjustedPath);
        if (!file.exists()) {
            // 如果指定的预览图不存在，返回默认预览图
            file = new File(previewPath);
        }
        
        if (!file.exists()) {
            throw new IllegalArgumentException("预览图文件不存在");
        }
        
        return new FileSystemResource(file);
    }

    // 异步处理方法

    /**
     * 异步处理文本生成任务
     */
    @Async
    public void processTextGenerationAsync(ModelTask task) {
        try {
            logger.info("开始处理文本生成任务: {}", task.getTaskId());
            
            // 更新状态为处理中
            task.setStatus(ModelTask.TaskStatus.PROCESSING);
            task.setProgress(10);
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 选择合适的AI服务并生成模型
            AIModelService selectedService = getSelectedAIService();
            String modelPath = selectedService.generateModelFromText(
                task.getInputText(), 
                task.getComplexity(), 
                task.getOutputFormat(),
                (progress) -> updateTaskProgress(task.getTaskId(), progress)
            );
            
            // 生成预览图
            String previewPath = aiModelService.generatePreviewImage(modelPath);
            
            // 更新任务状态
            task.setStatus(ModelTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setModelFilePath(modelPath);
            task.setPreviewImagePath(previewPath);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            logger.info("文本生成任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("文本生成任务失败: {}", task.getTaskId(), e);
            
            task.setStatus(ModelTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
        }
    }

    /**
     * 异步处理图片生成任务
     */
    @Async
    public void processImageGenerationAsync(ModelTask task) {
        try {
            logger.info("开始处理图片生成任务: {}", task.getTaskId());
            
            // 更新状态为处理中
            task.setStatus(ModelTask.TaskStatus.PROCESSING);
            task.setProgress(10);
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 选择合适的AI服务并生成模型
            AIModelService selectedService = getSelectedAIService();
            String modelPath = selectedService.generateModelFromImage(
                task.getInputImagePath(),
                task.getInputText(),
                task.getComplexity(),
                task.getOutputFormat(),
                (progress) -> updateTaskProgress(task.getTaskId(), progress)
            );
            
            // 生成预览图
            String previewPath = aiModelService.generatePreviewImage(modelPath);
            
            // 更新任务状态
            task.setStatus(ModelTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setModelFilePath(modelPath);
            task.setPreviewImagePath(previewPath);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            logger.info("图片生成任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("图片生成任务失败: {}", task.getTaskId(), e);
            
            task.setStatus(ModelTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
        }
    }

    // 辅助方法

    /**
     * 获取AI服务（Spring会根据配置自动注入正确的实现）
     */
    private AIModelService getSelectedAIService() {
        logger.info("使用AI服务: {}", aiModelService.getClass().getSimpleName());
        return aiModelService;
    }

    /**
     * 验证文本生成请求
     */
    private void validateTextRequest(TextGenerationRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("文本描述不能为空");
        }
        
        if (request.getText().length() > 1000) {
            throw new IllegalArgumentException("文本描述不能超过1000个字符");
        }
        
        if (request.getComplexity() == null) {
            throw new IllegalArgumentException("复杂度参数不能为空");
        }
        
        if (request.getFormat() == null) {
            throw new IllegalArgumentException("输出格式参数不能为空");
        }
    }

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片文件");
        }
        
        if (image.getSize() > maxFileSize) {
            throw new IllegalArgumentException("图片文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片格式，请上传JPG、PNG、GIF、BMP或WebP格式的图片");
        }
    }

    /**
     * 保存上传的图片
     */
    private String saveUploadedImage(MultipartFile image) {
        try {
            // 创建上传目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath);
            
            return filePath.toString();
            
        } catch (IOException e) {
            logger.error("保存上传图片失败", e);
            throw new RuntimeException("图片保存失败", e);
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 更新任务进度
     */
    private void updateTaskProgress(String taskId, int progress) {
        ModelTask task = modelTaskRepository.findByTaskId(taskId).orElse(null);
        if (task != null) {
            task.setProgress(progress);
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
        }
    }

    /**
     * 根据格式获取模型文件路径
     */
    private String getModelFilePathByFormat(ModelTask task, String format) {
        String basePath = task.getModelFilePath();
        if (basePath == null) return null;
        
        String baseDir = basePath.substring(0, basePath.lastIndexOf("."));
        return baseDir + "." + format.toLowerCase();
    }

    /**
     * 调整预览图路径
     */
    private String adjustPreviewPath(String basePath, String angle, String size) {
        String baseDir = basePath.substring(0, basePath.lastIndexOf("."));
        String extension = basePath.substring(basePath.lastIndexOf("."));
        
        return baseDir + "_" + angle + "_" + size + extension;
    }
}