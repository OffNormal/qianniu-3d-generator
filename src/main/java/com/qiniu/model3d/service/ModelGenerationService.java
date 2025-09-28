package com.qiniu.model3d.service;

import com.qiniu.model3d.dto.CacheResult;
import com.qiniu.model3d.dto.TextGenerationRequest;
import com.qiniu.model3d.dto.TaskEvaluationData;
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
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private CacheMetricsService cacheMetricsService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private ModelPreviewImageService modelPreviewImageService;

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

    @Value("${app.cache.similarity-threshold:0.8}")
    private double similarityThreshold;

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
        
        // 1. 缓存查找
        Optional<ModelTask> exactMatch = cacheService.findExactMatch(
            request.getText(), 
            ModelTask.TaskType.TEXT,
            request.getComplexity() != null ? request.getComplexity().toString() : null, 
            request.getFormat() != null ? request.getFormat().toString() : null
        );
        
        if (exactMatch.isPresent()) {
            ModelTask sourceTask = exactMatch.get();
            // 记录缓存命中
            cacheMetricsService.recordCacheHit(sourceTask.getId().toString(), "text_exact", 0);
            
            // 完全匹配缓存命中
            ModelTask cacheTask = createCacheTask(sourceTask, clientIp, 1.0);
            copyModelFilesAsync(sourceTask, cacheTask);
            
            logger.info("完全匹配缓存命中: taskId={}, sourceTaskId={}", 
                       cacheTask.getTaskId(), sourceTask.getTaskId());
            return cacheTask;
        }
        
        // 2. 相似度匹配查找
        List<CacheResult> similarMatches = cacheService.findSimilarMatches(
            request.getText(), 
            ModelTask.TaskType.TEXT,
            request.getComplexity() != null ? request.getComplexity().toString() : null, 
            request.getFormat() != null ? request.getFormat().toString() : null,
            similarityThreshold
        );
        
        if (!similarMatches.isEmpty()) {
            CacheResult bestMatch = similarMatches.get(0);
            // 使用SimilarityService的智能阈值判断
            if (similarityService.isHighSimilarity(bestMatch.getSimilarity()) || 
                similarityService.isExactMatch(bestMatch.getSimilarity())) {
                // 记录相似度缓存命中
                cacheMetricsService.recordCacheHit(bestMatch.getTask().getId().toString(), "text_similar", 0);
                
                ModelTask cacheTask = createCacheTask(bestMatch.getTask(), clientIp, bestMatch.getSimilarity());
                copyModelFilesAsync(bestMatch.getTask(), cacheTask);
                
                String level = similarityService.getSimilarityLevel(bestMatch.getSimilarity());
                logger.info("文本相似度缓存命中: taskId={}, sourceTaskId={}, similarity={}, level={}", 
                           cacheTask.getTaskId(), bestMatch.getTask().getTaskId(), bestMatch.getSimilarity(), level);
                return cacheTask;
            }
        }
        
        // 3. 缓存未命中 - 创建新任务
        // 记录缓存未命中
        cacheMetricsService.recordCacheMiss(request.getText(), "text", 0);
        
        ModelTask task = createNewTask(request, clientIp);
        
        // 异步处理生成任务
        processTextGenerationAsync(task);
        
        logger.info("缓存未命中，创建新任务: taskId={}, text={}", task.getTaskId(), request.getText());
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
        
        // 1. 图片缓存查找（基于文件内容和描述）
        String imageHash = calculateFileHash(imagePath);
        String combinedInput = (description != null ? description : "") + "|" + imageHash;
        
        Optional<ModelTask> exactMatch = cacheService.findExactMatch(
            combinedInput, 
            ModelTask.TaskType.IMAGE,
            complexity != null ? complexity.toString() : null,
            format != null ? format.toString() : null
        );
        
        if (exactMatch.isPresent()) {
            ModelTask sourceTask = exactMatch.get();
            // 记录图片缓存命中
            cacheMetricsService.recordCacheHit(sourceTask.getId().toString(), "image_exact", 0);
            
            // 完全匹配缓存命中
            ModelTask cacheTask = createImageCacheTask(sourceTask, clientIp, imagePath, description, 1.0);
            copyModelFilesAsync(sourceTask, cacheTask);
            
            logger.info("图片完全匹配缓存命中: taskId={}, sourceTaskId={}", 
                       cacheTask.getTaskId(), sourceTask.getTaskId());
            return cacheTask;
        }
        
        // 2. 相似度匹配查找（基于描述文本）
        if (description != null && !description.trim().isEmpty()) {
            List<CacheResult> similarMatches = cacheService.findSimilarMatches(
                description, 
                ModelTask.TaskType.IMAGE,
                complexity != null ? complexity.toString() : null,
                format != null ? format.toString() : null,
                similarityThreshold
            );
            
            if (!similarMatches.isEmpty()) {
                CacheResult bestMatch = similarMatches.get(0);
                // 对于图片，使用稍微宽松的相似度判断（包含中等相似度）
                if (similarityService.isHighSimilarity(bestMatch.getSimilarity()) || 
                    similarityService.isExactMatch(bestMatch.getSimilarity()) ||
                    similarityService.isMediumSimilarity(bestMatch.getSimilarity())) {
                    // 记录图片相似度缓存命中
                    cacheMetricsService.recordCacheHit(bestMatch.getTask().getId().toString(), "image_similar", 0);
                    
                    ModelTask cacheTask = createImageCacheTask(bestMatch.getTask(), clientIp, imagePath, description, bestMatch.getSimilarity());
                    copyModelFilesAsync(bestMatch.getTask(), cacheTask);
                    
                    String level = similarityService.getSimilarityLevel(bestMatch.getSimilarity());
                    logger.info("图片相似度缓存命中: taskId={}, sourceTaskId={}, similarity={}, level={}", 
                               cacheTask.getTaskId(), bestMatch.getTask().getTaskId(), bestMatch.getSimilarity(), level);
                    return cacheTask;
                }
            }
        }
        
        // 3. 缓存未命中 - 创建新任务
        // 记录图片缓存未命中
        cacheMetricsService.recordCacheMiss(description != null ? description : imagePath, "image", 0);
        
        ModelTask task = createNewImageTask(imagePath, description, complexity, format, clientIp);
        
        // 异步处理生成任务
        processImageGenerationAsync(task);
        
        logger.info("图片缓存未命中，创建新任务: taskId={}, imagePath={}", task.getTaskId(), imagePath);
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
            
            // 生成多张预览图
            List<String> previewPaths = selectedService.generateMultiplePreviewImages(modelPath, 3);
            
            // 保存预览图片到数据库
            for (int i = 0; i < previewPaths.size(); i++) {
                modelPreviewImageService.savePreviewImage(
                    task.getTaskId(), 
                    previewPaths.get(i), 
                    "MAIN", 
                    i + 1
                );
            }
            
            // 为了兼容性，设置第一张预览图为主预览图
            String mainPreviewPath = previewPaths.isEmpty() ? null : previewPaths.get(0);
            
            // 计算文件签名
            String fileSignature = cacheService.calculateFileSignature(modelPath);
            
            // 更新任务状态
            task.setStatus(ModelTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setModelFilePath(modelPath);
            task.setPreviewImagePath(mainPreviewPath);
            task.setFileSignature(fileSignature);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 更新TaskEvaluation状态为成功
            try {
                evaluationService.updateTaskStatus(task.getTaskId(), "SUCCESS", 
                    task.getCompletedAt(), null);
            } catch (Exception evalException) {
                logger.warn("更新TaskEvaluation状态失败: {}", task.getTaskId(), evalException);
            }
            
            // 缓存任务结果
            cacheService.cacheTask(task);
            
            logger.info("文本生成任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("文本生成任务失败: {}", task.getTaskId(), e);
            
            task.setStatus(ModelTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 更新TaskEvaluation状态为失败
            try {
                evaluationService.updateTaskStatus(task.getTaskId(), "FAILED", 
                    LocalDateTime.now(), e.getMessage());
            } catch (Exception evalException) {
                logger.warn("更新TaskEvaluation状态失败: {}", task.getTaskId(), evalException);
            }
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
            
            // 生成多张预览图
            List<String> previewPaths = selectedService.generateMultiplePreviewImages(modelPath, 3);
            
            // 保存预览图片到数据库
            for (int i = 0; i < previewPaths.size(); i++) {
                modelPreviewImageService.savePreviewImage(
                    task.getTaskId(), 
                    previewPaths.get(i), 
                    "MAIN", 
                    i + 1
                );
            }
            
            // 为了兼容性，设置第一张预览图为主预览图
            String mainPreviewPath = previewPaths.isEmpty() ? null : previewPaths.get(0);
            
            // 计算文件签名
            String fileSignature = cacheService.calculateFileSignature(modelPath);
            
            // 更新任务状态
            task.setStatus(ModelTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setModelFilePath(modelPath);
            task.setPreviewImagePath(mainPreviewPath);
            task.setFileSignature(fileSignature);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 更新TaskEvaluation状态为成功
            try {
                evaluationService.updateTaskStatus(task.getTaskId(), "SUCCESS", 
                    task.getCompletedAt(), null);
            } catch (Exception evalException) {
                logger.warn("更新TaskEvaluation状态失败: {}", task.getTaskId(), evalException);
            }
            
            // 缓存任务结果
            cacheService.cacheTask(task);
            
            logger.info("图片生成任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("图片生成任务失败: {}", task.getTaskId(), e);
            
            task.setStatus(ModelTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            modelTaskRepository.save(task);
            
            // 更新TaskEvaluation状态为失败
            try {
                evaluationService.updateTaskStatus(task.getTaskId(), "FAILED", 
                    LocalDateTime.now(), e.getMessage());
            } catch (Exception evalException) {
                logger.warn("更新TaskEvaluation状态失败: {}", task.getTaskId(), evalException);
            }
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

    /**
     * 创建缓存任务
     */
    private ModelTask createCacheTask(ModelTask sourceTask, String clientIp, double similarity) {
        ModelTask cacheTask = new ModelTask();
        cacheTask.setTaskId(generateTaskId());
        cacheTask.setType(sourceTask.getType());
        cacheTask.setInputText(sourceTask.getInputText());
        cacheTask.setInputImagePath(sourceTask.getInputImagePath());
        cacheTask.setComplexity(sourceTask.getComplexity());
        cacheTask.setOutputFormat(sourceTask.getOutputFormat());
        cacheTask.setStatus(ModelTask.TaskStatus.PROCESSING);
        cacheTask.setProgress(90); // 缓存任务进度设为90%，等待文件复制完成
        cacheTask.setClientIp(clientIp);
        cacheTask.setCreatedAt(LocalDateTime.now());
        cacheTask.setUpdatedAt(LocalDateTime.now());
        
        // 设置缓存相关字段
        cacheTask.setInputHash(cacheService.calculateInputHash(sourceTask.getInputText(), 
                                                               sourceTask.getType(),
                                                               sourceTask.getComplexity() != null ? sourceTask.getComplexity().toString() : null, 
                                                               sourceTask.getOutputFormat() != null ? sourceTask.getOutputFormat().toString() : null));
        
        // 保存任务
        cacheTask = modelTaskRepository.save(cacheTask);
        
        // 更新源任务的引用计数和相似度使用计数
        cacheService.updateCacheAccess(sourceTask.getTaskId());
        if (similarity < 1.0) {
            // 如果不是精确匹配，更新相似度使用计数
            cacheService.updateSimilarityUsage(sourceTask.getTaskId());
        }
        
        return cacheTask;
    }

    /**
     * 创建新任务
     */
    private ModelTask createNewTask(TextGenerationRequest request, String clientIp) {
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
        
        // 设置缓存相关字段
        task.setInputHash(cacheService.calculateInputHash(request.getText(), 
                                                          ModelTask.TaskType.TEXT,
                                                          request.getComplexity() != null ? request.getComplexity().toString() : null, 
                                                          request.getFormat() != null ? request.getFormat().toString() : null));
        
        // 保存任务
        ModelTask savedTask = modelTaskRepository.save(task);
        
        // 创建对应的TaskEvaluation记录
        TaskEvaluationData evaluationData = new TaskEvaluationData();
        evaluationData.setPrompt(request.getText());
        evaluationData.setResultFormat(request.getFormat() != null ? request.getFormat().toString() : null);
        evaluationData.setStatus("PENDING");
        evaluationData.setClientIp(clientIp);
        evaluationData.setSubmitTime(LocalDateTime.now());
        
        evaluationService.recordTaskEvaluation(savedTask.getTaskId(), evaluationData);
        
        return savedTask;
    }

    /**
     * 异步复制模型文件
     */
    private void copyModelFilesAsync(ModelTask sourceTask, ModelTask targetTask) {
        CompletableFuture.runAsync(() -> {
            try {
                // 复制模型文件
                String sourceModelPath = sourceTask.getModelFilePath();
                String sourcePreviewPath = sourceTask.getPreviewImagePath();
                
                if (sourceModelPath != null && Files.exists(Paths.get(sourceModelPath))) {
                    String targetModelPath = generateTargetFilePath(sourceModelPath, targetTask.getTaskId());
                    Files.copy(Paths.get(sourceModelPath), Paths.get(targetModelPath));
                    targetTask.setModelFilePath(targetModelPath);
                }
                
                if (sourcePreviewPath != null && Files.exists(Paths.get(sourcePreviewPath))) {
                    String targetPreviewPath = generateTargetFilePath(sourcePreviewPath, targetTask.getTaskId());
                    Files.copy(Paths.get(sourcePreviewPath), Paths.get(targetPreviewPath));
                    targetTask.setPreviewImagePath(targetPreviewPath);
                }
                
                // 计算文件签名
                if (targetTask.getModelFilePath() != null) {
                    String fileSignature = cacheService.calculateFileSignature(targetTask.getModelFilePath());
                    targetTask.setFileSignature(fileSignature);
                }
                
                // 更新任务状态为完成
                targetTask.setStatus(ModelTask.TaskStatus.COMPLETED);
                targetTask.setProgress(100);
                targetTask.setCompletedAt(LocalDateTime.now());
                targetTask.setUpdatedAt(LocalDateTime.now());
                modelTaskRepository.save(targetTask);
                
                logger.info("缓存文件复制完成: taskId={}", targetTask.getTaskId());
                
            } catch (Exception e) {
                logger.error("缓存文件复制失败: taskId={}", targetTask.getTaskId(), e);
                
                targetTask.setStatus(ModelTask.TaskStatus.FAILED);
                targetTask.setErrorMessage("文件复制失败: " + e.getMessage());
                targetTask.setUpdatedAt(LocalDateTime.now());
                modelTaskRepository.save(targetTask);
            }
        });
    }

    /**
     * 生成目标文件路径
     */
    private String generateTargetFilePath(String sourcePath, String taskId) {
        Path source = Paths.get(sourcePath);
        String fileName = source.getFileName().toString();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = taskId + extension;
        
        return source.getParent().resolve(newFileName).toString();
    }

    /**
     * 创建图片缓存任务
     */
    private ModelTask createImageCacheTask(ModelTask sourceTask, String clientIp, String imagePath, String description, double similarity) {
        ModelTask cacheTask = new ModelTask();
        cacheTask.setTaskId(generateTaskId());
        cacheTask.setType(ModelTask.TaskType.IMAGE);
        cacheTask.setInputImagePath(imagePath);
        cacheTask.setInputText(description);
        cacheTask.setComplexity(sourceTask.getComplexity());
        cacheTask.setOutputFormat(sourceTask.getOutputFormat());
        cacheTask.setStatus(ModelTask.TaskStatus.PROCESSING);
        cacheTask.setProgress(90);
        cacheTask.setClientIp(clientIp);
        cacheTask.setCreatedAt(LocalDateTime.now());
        cacheTask.setUpdatedAt(LocalDateTime.now());
        
        // 设置缓存相关字段
        String imageHash = cacheService.calculateFileSignature(imagePath);
        String combinedInput = (description != null ? description : "") + "|" + imageHash;
        cacheTask.setInputHash(cacheService.calculateInputHash(combinedInput, 
                                                               sourceTask.getType(),
                                                               sourceTask.getComplexity() != null ? sourceTask.getComplexity().toString() : null, 
                                                               sourceTask.getOutputFormat() != null ? sourceTask.getOutputFormat().toString() : null));
        
        // 保存任务
        cacheTask = modelTaskRepository.save(cacheTask);
        
        // 更新源任务的引用计数和相似度使用计数
        cacheService.updateCacheAccess(sourceTask.getTaskId());
        if (similarity < 1.0) {
            // 如果不是精确匹配，更新相似度使用计数
            cacheService.updateSimilarityUsage(sourceTask.getTaskId());
        }
        
        return cacheTask;
    }

    /**
     * 创建新图片任务
     */
    private ModelTask createNewImageTask(String imagePath, String description, ModelTask.Complexity complexity, 
                                       ModelTask.OutputFormat format, String clientIp) {
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
        
        // 设置缓存相关字段
        String imageHash = cacheService.calculateFileSignature(imagePath);
        String combinedInput = (description != null ? description : "") + "|" + imageHash;
        task.setInputHash(cacheService.calculateInputHash(combinedInput, 
                                                          ModelTask.TaskType.IMAGE,
                                                          complexity != null ? complexity.toString() : null, 
                                                          format != null ? format.toString() : null));
        
        // 保存任务
        task = modelTaskRepository.save(task);
        
        // 创建TaskEvaluation记录
        try {
            TaskEvaluationData evaluationData = new TaskEvaluationData();
            evaluationData.setJobId(task.getTaskId());
            evaluationData.setPrompt(description != null ? description : "图片生成任务");
            evaluationData.setResultFormat(format != null ? format.toString() : "OBJ");
            evaluationData.setStatus("PENDING");
            evaluationData.setClientIp(clientIp);
            evaluationData.setSubmitTime(task.getCreatedAt());
            
            evaluationService.recordTaskEvaluation(task.getTaskId(), evaluationData);
        } catch (Exception e) {
            logger.warn("创建TaskEvaluation记录失败: {}", task.getTaskId(), e);
        }
        
        return task;
    }

    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("计算文件哈希失败: {}", filePath, e);
            return filePath; // 降级处理
        }
    }

    /**
     * 获取模型预览图片路径
     */
    public String getModelPreviewPath(String modelId) {
        ModelTask task = modelTaskRepository.findByTaskId(modelId).orElse(null);
        return task != null ? task.getPreviewImagePath() : null;
    }

    /**
     * 获取特定的模型预览图片路径
     */
    public String getSpecificModelPreviewPath(String modelId, Long imageId) {
        ModelTask task = modelTaskRepository.findByTaskId(modelId).orElse(null);
        if (task == null) {
            return null;
        }

        // 通过ModelPreviewImageService查找特定的预览图片
        return modelPreviewImageService.getPreviewImagePath(task.getTaskId(), imageId);
    }
}