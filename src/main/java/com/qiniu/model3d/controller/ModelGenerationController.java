package com.qiniu.model3d.controller;

import com.qiniu.model3d.dto.ApiResponse;
import com.qiniu.model3d.dto.TaskStatusResponse;
import com.qiniu.model3d.dto.TextGenerationRequest;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.service.ModelGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

/**
 * 3D模型生成控制器
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/models")
@CrossOrigin(origins = "*")
public class ModelGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(ModelGenerationController.class);

    @Autowired
    private ModelGenerationService modelGenerationService;

    /**
     * 根据文本生成3D模型
     */
    @PostMapping("/generate/text")
    public ApiResponse<Map<String, Object>> generateFromText(
            @Valid @RequestBody TextGenerationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("收到文本生成请求: {}", request.getText());
            
            String clientIp = getClientIp(httpRequest);
            ModelTask task = modelGenerationService.generateFromText(request, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", task.getTaskId());
            response.put("status", task.getStatus());
            response.put("estimatedTime", getEstimatedTime(task.getComplexity(), task.getType()));
            
            return ApiResponse.success("模型生成任务已创建", response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("文本生成请求参数错误: {}", e.getMessage());
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("文本生成请求处理失败", e);
            return ApiResponse.error("服务器内部错误，请稍后重试");
        }
    }

    /**
     * 根据图片生成3D模型
     */
    @PostMapping("/generate/image")
    public ApiResponse<Map<String, Object>> generateFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "complexity", defaultValue = "SIMPLE") String complexity,
            @RequestParam(value = "format", defaultValue = "OBJ") String format,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("收到图片生成请求: 文件名={}, 大小={}", image.getOriginalFilename(), image.getSize());
            
            String clientIp = getClientIp(httpRequest);
            ModelTask task = modelGenerationService.generateFromImage(
                image, 
                ModelTask.Complexity.valueOf(complexity.toUpperCase()),
                ModelTask.OutputFormat.valueOf(format.toUpperCase()),
                description,
                clientIp
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", task.getTaskId());
            response.put("status", task.getStatus());
            response.put("estimatedTime", getEstimatedTime(task.getComplexity(), task.getType()));
            
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("filename", image.getOriginalFilename());
            imageInfo.put("size", image.getSize());
            imageInfo.put("fileSize", formatFileSize(image.getSize()));
            response.put("imageInfo", imageInfo);
            
            return ApiResponse.success("图片上传成功，开始生成模型", response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("图片生成请求参数错误: {}", e.getMessage());
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("图片生成请求处理失败", e);
            return ApiResponse.error("服务器内部错误，请稍后重试");
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public ApiResponse<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        try {
            logger.debug("查询任务状态: {}", taskId);
            
            ModelTask task = modelGenerationService.getTaskStatus(taskId);
            if (task == null) {
                return ApiResponse.notFound("任务不存在");
            }
            
            TaskStatusResponse response = new TaskStatusResponse(task);
            return ApiResponse.success("查询成功", response);
            
        } catch (Exception e) {
            logger.error("查询任务状态失败: taskId={}", taskId, e);
            return ApiResponse.error("查询失败，请稍后重试");
        }
    }

    /**
     * 获取生成历史记录
     */
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> getGenerationHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        
        try {
            logger.debug("获取生成历史记录: page={}, size={}, status={}", page, size, status);
            
            // 验证分页参数
            if (page < 1) page = 1;
            if (size < 1 || size > 50) size = 10;
            
            String clientIp = getClientIp(httpRequest);
            
            // 解析状态参数
            ModelTask.TaskStatus taskStatus = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    taskStatus = ModelTask.TaskStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.badRequest("无效的状态参数: " + status);
                }
            }
            
            // 获取历史记录
            Page<ModelTask> taskPage = modelGenerationService.getGenerationHistory(clientIp, page, size, taskStatus);
            
            // 转换为响应格式
            List<Map<String, Object>> items = taskPage.getContent().stream()
                .map(this::convertTaskToHistoryItem)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", taskPage.getTotalElements());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", taskPage.getTotalPages());
            response.put("items", items);
            
            return ApiResponse.success("查询成功", response);
            
        } catch (Exception e) {
            logger.error("获取生成历史记录失败", e);
            return ApiResponse.error("查询失败，请稍后重试");
        }
    }

    /**
     * 下载模型文件
     */
    @GetMapping("/download/{modelId}")
    public ResponseEntity<Resource> downloadModel(
            @PathVariable String modelId,
            @RequestParam(value = "format", required = false) String format) {
        
        try {
            logger.info("下载模型文件: modelId={}, format={}", modelId, format);
            
            Resource resource = modelGenerationService.downloadModel(modelId, format);
            
            String filename = resource.getFilename();
            String contentType = "application/octet-stream";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (IllegalArgumentException e) {
            logger.warn("下载模型文件参数错误: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("下载模型文件失败: modelId={}", modelId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取模型预览图
     */
    @GetMapping("/preview/{modelId}")
    public ResponseEntity<Resource> getModelPreview(
            @PathVariable String modelId,
            @RequestParam(value = "angle", defaultValue = "front") String angle,
            @RequestParam(value = "size", defaultValue = "medium") String size) {
        
        try {
            logger.debug("获取模型预览图: modelId={}, angle={}, size={}", modelId, angle, size);
            
            Resource resource = modelGenerationService.getModelPreview(modelId, angle, size);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
                    
        } catch (IllegalArgumentException e) {
            logger.warn("获取模型预览图参数错误: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("获取模型预览图失败: modelId={}", modelId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取特定的模型预览图片
     */
    @GetMapping("/preview/{modelId}/{imageId}")
    public ResponseEntity<Resource> getSpecificModelPreview(
            @PathVariable String modelId, 
            @PathVariable Long imageId) {
        try {
            String previewPath = modelGenerationService.getSpecificModelPreviewPath(modelId, imageId);
            if (previewPath == null) {
                return ResponseEntity.notFound().build();
            }

            Path path = Paths.get(previewPath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (Exception e) {
            logger.error("获取特定模型预览图片失败: modelId={}, imageId={}, error={}", 
                        modelId, imageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 辅助方法

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 转换任务数据为历史记录项
     */
    private Map<String, Object> convertTaskToHistoryItem(ModelTask task) {
        Map<String, Object> item = new HashMap<>();
        item.put("taskId", task.getTaskId());
        item.put("prompt", task.getInputText()); // 文本输入
        item.put("status", task.getStatus().toString());
        item.put("createdAt", task.getCreatedAt());
        item.put("completedAt", task.getCompletedAt());
        item.put("modelUrl", task.getModelFilePath()); // 模型文件路径
        item.put("previewUrl", task.getPreviewImagePath()); // 预览图片路径
        item.put("errorMessage", task.getErrorMessage());
        
        // 计算耗时（如果任务已完成）
        if (task.getCompletedAt() != null && task.getCreatedAt() != null) {
            long duration = java.time.Duration.between(task.getCreatedAt(), task.getCompletedAt()).getSeconds();
            item.put("duration", duration);
        }
        
        return item;
    }

    /**
     * 获取预估生成时间（秒）
     */
    private int getEstimatedTime(ModelTask.Complexity complexity, ModelTask.TaskType type) {
        int baseTime = type == ModelTask.TaskType.TEXT ? 30 : 60;
        
        switch (complexity) {
            case SIMPLE:
                return baseTime;
            case MEDIUM:
                return baseTime * 2;
            case COMPLEX:
                return baseTime * 4;
            default:
                return baseTime;
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        }
    }
}