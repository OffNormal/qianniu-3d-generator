package com.qiniu.model3d.controller;

import com.qiniu.model3d.dto.*;
import com.qiniu.model3d.service.TencentAi3dClient;
import com.qiniu.model3d.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI3D 控制器
 * 提供腾讯混元生3D相关的REST API接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/ai3d")
public class Ai3dController {

    private static final Logger logger = LoggerFactory.getLogger(Ai3dController.class);

    @Autowired
    private TencentAi3dClient tencentAi3dClient;
    
    @Autowired
    private EvaluationService evaluationService;

    /**
     * 提交混元生3D任务
     * 
     * @param request 提交任务请求
     * @return 提交任务响应
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<SubmitHunyuanTo3DJobResponse>> submitJob(
            @RequestBody SubmitHunyuanTo3DJobRequest request) {
        try {
            logger.info("收到提交3D生成任务请求");
            SubmitHunyuanTo3DJobResponse response = tencentAi3dClient.submitHunyuanTo3DJob(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("提交3D生成任务失败", e);
            return ResponseEntity.ok(ApiResponse.error("提交任务失败: " + e.getMessage()));
        }
    }

    /**
     * 根据文本提交3D生成任务
     * 
     * @param prompt 文本描述
     * @param resultFormat 结果格式（可选）
     * @param enablePBR 是否启用PBR（可选）
     * @param request HTTP请求对象
     * @return 提交任务响应
     */
    @PostMapping("/generate/text")
    public ResponseEntity<ApiResponse<SubmitHunyuanTo3DJobResponse>> submitTextTo3D(
            @RequestParam String prompt,
            @RequestParam(required = false) String resultFormat,
            @RequestParam(required = false) Boolean enablePBR,
            HttpServletRequest request) {
        
        LocalDateTime submitTime = LocalDateTime.now();
        String clientIp = getClientIpAddress(request);
        
        try {
            logger.info("收到文本生成3D任务请求: {}", prompt);
            SubmitHunyuanTo3DJobResponse response = tencentAi3dClient.submitTextTo3DJob(prompt, resultFormat, enablePBR);
            
            // 记录任务评估数据
            if (response != null && response.getJobId() != null) {
                String jobId = response.getJobId();
                evaluationService.recordTaskSubmission(jobId, prompt, resultFormat, submitTime, clientIp);
                logger.info("记录任务提交数据: jobId={}, prompt={}, format={}", jobId, prompt, resultFormat);
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("提交文本生成3D任务失败", e);
            return ResponseEntity.ok(ApiResponse.error("提交任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
     * 用户评分接口
     */
    @PostMapping("/feedback/{jobId}")
    public ResponseEntity<ApiResponse<String>> submitUserFeedback(
            @PathVariable String jobId,
            @RequestParam int rating,
            HttpServletRequest request) {
        
        try {
            // 验证评分范围
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(ApiResponse.error("评分必须在1-5之间"));
            }
            
            String clientIp = getClientIpAddress(request);
            logger.info("收到用户反馈: jobId={}, rating={}, clientIp={}", jobId, rating, clientIp);
            
            // 更新用户反馈
            evaluationService.updateUserFeedback(jobId, rating);
            
            return ResponseEntity.ok(ApiResponse.success("感谢您的反馈！", "感谢您的反馈！"));
        } catch (Exception e) {
            logger.error("提交用户反馈失败: jobId={}, rating={}", jobId, rating, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("提交反馈失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务评估统计信息
     */
    @GetMapping("/evaluation/stats")
    public ResponseEntity<ApiResponse<EvaluationMetrics>> getEvaluationStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            EvaluationMetrics metrics = evaluationService.getMetrics(start, end);
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            logger.error("获取评估统计信息失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 根据图片URL提交3D生成任务
     * 
     * @param imageUrl 图片URL
     * @param resultFormat 结果格式（可选）
     * @param enablePBR 是否启用PBR（可选）
     * @return 提交任务响应
     */
    @PostMapping("/submit/image-url")
    public ResponseEntity<ApiResponse<SubmitHunyuanTo3DJobResponse>> submitImageUrlJob(
            @RequestParam String imageUrl,
            @RequestParam(required = false) String resultFormat,
            @RequestParam(required = false) Boolean enablePBR,
            HttpServletRequest request) {
        
        LocalDateTime submitTime = LocalDateTime.now();
        String clientIp = getClientIpAddress(request);
        
        try {
            logger.info("收到图片URL生成3D任务请求: {}", imageUrl);
            SubmitHunyuanTo3DJobResponse response = tencentAi3dClient.submitImageUrlTo3DJob(imageUrl, resultFormat, enablePBR);
            
            // 记录任务评估数据
            if (response != null && response.getJobId() != null) {
                String jobId = response.getJobId();
                evaluationService.recordTaskSubmission(jobId, "图片URL: " + imageUrl, resultFormat, submitTime, clientIp);
                logger.info("记录任务提交数据: jobId={}, imageUrl={}, format={}", jobId, imageUrl, resultFormat);
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("提交图片URL生成3D任务失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("提交任务失败: " + e.getMessage()));
        }
    }

    /**
     * 根据图片Base64提交3D生成任务
     * 
     * @param imageBase64 图片Base64数据
     * @param resultFormat 结果格式（可选）
     * @param enablePBR 是否启用PBR（可选）
     * @return 提交任务响应
     */
    @PostMapping("/submit/image-base64")
    public ResponseEntity<ApiResponse<SubmitHunyuanTo3DJobResponse>> submitImageBase64Job(
            @RequestParam String imageBase64,
            @RequestParam(required = false) String resultFormat,
            @RequestParam(required = false) Boolean enablePBR,
            HttpServletRequest request) {
        
        LocalDateTime submitTime = LocalDateTime.now();
        String clientIp = getClientIpAddress(request);
        
        try {
            logger.info("收到图片Base64生成3D任务请求");
            SubmitHunyuanTo3DJobResponse response = tencentAi3dClient.submitImageBase64To3DJob(imageBase64, resultFormat, enablePBR);
            
            // 记录任务评估数据
            if (response != null && response.getJobId() != null) {
                String jobId = response.getJobId();
                evaluationService.recordTaskSubmission(jobId, "图片Base64上传", resultFormat, submitTime, clientIp);
                logger.info("记录任务提交数据: jobId={}, format={}", jobId, resultFormat);
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("提交图片Base64生成3D任务失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("提交任务失败: " + e.getMessage()));
        }
    }

    /**
     * 查询任务状态
     * 
     * @param jobId 任务ID
     * @return 查询任务响应
     */
    @GetMapping("/query/{jobId}")
    public ResponseEntity<ApiResponse<QueryHunyuanTo3DJobResponse>> queryJob(@PathVariable String jobId) {
        
        try {
            logger.info("查询任务状态: jobId={}", jobId);
            QueryHunyuanTo3DJobResponse response = tencentAi3dClient.queryHunyuanTo3DJob(jobId);
            
            // 更新任务状态和完成信息
            if (response != null) {
                evaluationService.updateTaskStatus(jobId, response);
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("查询任务状态失败: jobId={}", jobId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("查询任务失败: " + e.getMessage()));
        }
    }

    /**
     * 轮询任务直到完成
     * 
     * @param jobId 任务ID
     * @param maxWaitTimeSeconds 最大等待时间（秒），默认300秒
     * @param pollIntervalSeconds 轮询间隔（秒），默认5秒
     * @return 最终任务响应
     */
    @GetMapping("/poll/{jobId}")
    public ResponseEntity<ApiResponse<QueryHunyuanTo3DJobResponse>> pollJob(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "300") int maxWaitTimeSeconds,
            @RequestParam(defaultValue = "5") int pollIntervalSeconds) {
        try {
            logger.info("收到轮询任务请求: {}", jobId);
            QueryHunyuanTo3DJobResponse response = tencentAi3dClient.pollJobUntilComplete(jobId, maxWaitTimeSeconds, pollIntervalSeconds);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("轮询任务失败", e);
            return ResponseEntity.ok(ApiResponse.error("轮询任务失败: " + e.getMessage()));
        }
    }

    /**
     * 检查服务状态
     * 
     * @return 服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getServiceStatus() {
        try {
            boolean available = tencentAi3dClient.isServiceAvailable();
            String status = available ? "服务可用" : "服务不可用";
            String clientInfo = tencentAi3dClient.getClientInfo();
            return ResponseEntity.ok(ApiResponse.success("服务状态", status + " - " + clientInfo));
        } catch (Exception e) {
            logger.error("检查服务状态失败", e);
            return ResponseEntity.ok(ApiResponse.<String>error("检查服务状态失败: " + e.getMessage()));
        }
    }

    /**
     * 下载模型文件
     * 
     * @param jobId 任务ID
     * @param format 文件格式（可选）
     * @return 模型文件
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> downloadModel(
            @PathVariable String jobId,
            @RequestParam(value = "format", required = false) String format) {
        
        try {
            logger.info("下载模型文件: jobId={}, format={}", jobId, format);
            
            // 查询任务状态获取文件信息
            QueryHunyuanTo3DJobResponse response = tencentAi3dClient.queryHunyuanTo3DJob(jobId);
            
            if (response == null || !response.isCompleted()) {
                logger.warn("任务未完成或不存在: jobId={}", jobId);
                return ResponseEntity.notFound().build();
            }
            
            if (response.getResultFile3Ds() == null || response.getResultFile3Ds().isEmpty()) {
                logger.warn("任务没有生成文件: jobId={}", jobId);
                return ResponseEntity.notFound().build();
            }
            
            // 查找匹配格式的文件，如果没有指定格式则使用第一个文件
            File3D targetFile = null;
            if (format != null && !format.isEmpty()) {
                for (File3D file3D : response.getResultFile3Ds()) {
                    if (file3D.getType() != null && file3D.getType().equalsIgnoreCase(format)) {
                        targetFile = file3D;
                        break;
                    }
                }
            }
            
            if (targetFile == null && !response.getResultFile3Ds().isEmpty()) {
                targetFile = response.getResultFile3Ds().get(0);
            }
            
            if (targetFile == null || targetFile.getUrl() == null) {
                logger.warn("找不到可下载的文件: jobId={}, format={}", jobId, format);
                return ResponseEntity.notFound().build();
            }
            
            // 直接下载文件内容
            logger.info("开始下载文件: url={}", targetFile.getUrl());
            byte[] fileContent = downloadFileFromUrl(targetFile.getUrl());
            
            if (fileContent == null || fileContent.length == 0) {
                logger.warn("下载的文件内容为空: url={}", targetFile.getUrl());
                return ResponseEntity.notFound().build();
            }
            
            // 生成文件名
            String filename = "model_" + jobId;
            String fileExtension = getFileExtensionFromUrl(targetFile.getUrl());
            
            if (targetFile.getType() != null && !targetFile.getType().isEmpty()) {
                filename += "." + targetFile.getType().toLowerCase();
            } else if (fileExtension != null && !fileExtension.isEmpty()) {
                filename += "." + fileExtension;
            } else {
                filename += ".zip"; // 默认为zip格式
            }
            
            String contentType = "application/octet-stream";
            
            // 记录下载行为
            evaluationService.recordDownload(jobId, fileContent.length);
            
            // 创建资源对象
            Resource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            logger.error("文件下载错误: jobId={}", jobId, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("下载模型文件失败: jobId={}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 从URL下载文件内容
     * 
     * @param fileUrl 文件URL
     * @return 文件字节数组
     * @throws IOException 下载失败时抛出异常
     */
    private byte[] downloadFileFromUrl(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            connection.setInstanceFollowRedirects(true);
            
            // 设置User-Agent，避免某些服务器拒绝请求
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("下载文件失败，HTTP状态码: {}, URL: {}", responseCode, fileUrl);
                return null;
            }
            
            // 读取文件内容
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] result = outputStream.toByteArray();
                logger.info("文件下载成功，大小: {} bytes", result.length);
                return result;
            }
            
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 从URL中提取文件扩展名
     * 
     * @param url 文件URL
     * @return 文件扩展名（不包含点号）
     */
    private String getFileExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 移除查询参数
            int queryIndex = url.indexOf('?');
            String cleanUrl = queryIndex > 0 ? url.substring(0, queryIndex) : url;
            
            // 提取文件名
            int lastSlashIndex = cleanUrl.lastIndexOf('/');
            String filename = lastSlashIndex > 0 ? cleanUrl.substring(lastSlashIndex + 1) : cleanUrl;
            
            // 提取扩展名
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
                return filename.substring(lastDotIndex + 1).toLowerCase();
            }
            
        } catch (Exception e) {
            logger.warn("提取文件扩展名失败: url={}", url, e);
        }
        
        return null;
    }

    /**
     * 获取模型预览图
     * 
     * @param jobId 任务ID
     * @param angle 视角参数（兼容前端，实际不使用）
     * @param size 尺寸参数（兼容前端，实际不使用）
     * @return 预览图响应
     */
    @GetMapping("/preview/{jobId}")
    public ResponseEntity<?> getModelPreview(
            @PathVariable String jobId,
            @RequestParam(value = "angle", defaultValue = "front") String angle,
            @RequestParam(value = "size", defaultValue = "medium") String size) {
        
        try {
            logger.info("获取模型预览图: jobId={}, angle={}, size={}", jobId, angle, size);
            
            // 查询腾讯云任务状态
            QueryHunyuanTo3DJobResponse response = tencentAi3dClient.queryHunyuanTo3DJob(jobId);
            
            if (response == null || response.getStatus() == null) {
                logger.warn("查询任务状态失败: jobId={}", jobId);
                return ResponseEntity.notFound().build();
            }
            
            // 检查任务状态
            if (!"DONE".equals(response.getStatus())) {
                logger.warn("任务未完成，无法获取预览图: jobId={}, status={}", jobId, response.getStatus());
                return ResponseEntity.badRequest().build();
            }
            
            // 获取预览图URL
            String previewImageUrl = null;
            if (response.getResultFile3Ds() != null && !response.getResultFile3Ds().isEmpty()) {
                for (File3D file : response.getResultFile3Ds()) {
                    if (file.getPreviewImageUrl() != null && !file.getPreviewImageUrl().isEmpty()) {
                        previewImageUrl = file.getPreviewImageUrl();
                        break;
                    }
                }
            }
            
            if (previewImageUrl == null || previewImageUrl.isEmpty()) {
                logger.warn("预览图URL不存在: jobId={}", jobId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("获取到预览图URL: {}", previewImageUrl);
            
            // 下载预览图并返回
            byte[] imageContent = downloadFileFromUrl(previewImageUrl);
            
            if (imageContent == null || imageContent.length == 0) {
                logger.warn("下载预览图失败: url={}", previewImageUrl);
                return ResponseEntity.notFound().build();
            }
            
            // 记录预览行为
            evaluationService.recordPreview(jobId);
            
            // 创建资源对象
            Resource resource = new ByteArrayResource(imageContent);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("获取模型预览图失败: jobId={}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}