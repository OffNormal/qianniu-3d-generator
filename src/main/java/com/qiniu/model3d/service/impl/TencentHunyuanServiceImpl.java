package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.dto.QueryHunyuanTo3DJobResponse;
import com.qiniu.model3d.dto.SubmitHunyuanTo3DJobResponse;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.service.AIModelService;
import com.qiniu.model3d.service.TencentAi3dClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 腾讯混元AI3D服务实现类
 * 集成腾讯云混元生3D服务，提供真实的AI模型生成功能
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service("tencentHunyuanService")
@ConditionalOnProperty(name = "app.ai.service-type", havingValue = "tencent")
public class TencentHunyuanServiceImpl implements AIModelService {

    private static final Logger logger = LoggerFactory.getLogger(TencentHunyuanServiceImpl.class);

    @Autowired
    private TencentAi3dClient tencentAi3dClient;

    @Value("${app.file.model-dir}")
    private String modelDir;

    @Value("${app.file.preview-dir}")
    private String previewDir;

    @Value("${tencent.cloud.ai3d.retry-count:60}")
    private int maxRetryCount;

    @Value("${tencent.cloud.ai3d.poll-interval:5}")
    private int pollIntervalSeconds;

    @Override
    public String generateModelFromText(String text, 
                                      ModelTask.Complexity complexity, 
                                      ModelTask.OutputFormat format,
                                      Consumer<Integer> progressCallback) throws Exception {
        
        logger.info("开始使用腾讯混元服务根据文本生成3D模型: text={}, complexity={}, format={}", 
                   text, complexity, format);
        
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("文本描述不能为空");
        }

        try {
            // 设置初始进度
            progressCallback.accept(10);

            // 提交3D生成任务
            String resultFormat = convertToTencentFormat(format);
            SubmitHunyuanTo3DJobResponse submitResponse = tencentAi3dClient.submitTextTo3DJob(
                text, resultFormat, true
            );

            if (submitResponse == null || !StringUtils.hasText(submitResponse.getJobId())) {
                throw new RuntimeException("提交腾讯混元3D生成任务失败");
            }

            String jobId = submitResponse.getJobId();
            logger.info("腾讯混元3D生成任务已提交，任务ID: {}", jobId);
            
            progressCallback.accept(20);

            // 轮询任务状态直到完成
            QueryHunyuanTo3DJobResponse queryResponse = pollJobWithProgress(
                jobId, maxRetryCount * pollIntervalSeconds, pollIntervalSeconds, progressCallback
            );

            if (queryResponse == null || !queryResponse.isCompleted()) {
                String errorMsg = queryResponse != null ? queryResponse.getErrorMessage() : "未知错误";
                throw new RuntimeException("腾讯混元3D生成失败: " + errorMsg);
            }

            // 下载并保存模型文件
            String modelPath = downloadAndSaveModel(queryResponse, format, text);
            
            progressCallback.accept(100);
            logger.info("腾讯混元文本生成3D模型完成: {}", modelPath);
            return modelPath;

        } catch (Exception e) {
            logger.error("腾讯混元API调用失败", e);
            throw new Exception("腾讯混元3D模型生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateModelFromImage(String imagePath,
                                       String description,
                                       ModelTask.Complexity complexity,
                                       ModelTask.OutputFormat format,
                                       Consumer<Integer> progressCallback) throws Exception {
        
        logger.info("开始使用腾讯混元服务根据图片生成3D模型: imagePath={}, description={}, complexity={}, format={}", 
                   imagePath, description, complexity, format);
        
        if (!StringUtils.hasText(imagePath)) {
            throw new IllegalArgumentException("图片路径不能为空");
        }

        try {
            // 设置初始进度
            progressCallback.accept(10);

            // 读取图片并转换为Base64
            String imageBase64 = convertImageToBase64(imagePath);
            
            progressCallback.accept(15);

            // 提交3D生成任务
            String resultFormat = convertToTencentFormat(format);
            SubmitHunyuanTo3DJobResponse submitResponse = tencentAi3dClient.submitImageBase64To3DJob(
                imageBase64, resultFormat, true
            );

            if (submitResponse == null || !StringUtils.hasText(submitResponse.getJobId())) {
                throw new RuntimeException("提交腾讯混元3D生成任务失败");
            }

            String jobId = submitResponse.getJobId();
            logger.info("腾讯混元3D生成任务已提交，任务ID: {}", jobId);
            
            progressCallback.accept(25);

            // 轮询任务状态直到完成
            QueryHunyuanTo3DJobResponse queryResponse = pollJobWithProgress(
                jobId, maxRetryCount * pollIntervalSeconds, pollIntervalSeconds, progressCallback
            );

            if (queryResponse == null || !queryResponse.isCompleted()) {
                String errorMsg = queryResponse != null ? queryResponse.getErrorMessage() : "未知错误";
                throw new RuntimeException("腾讯混元3D生成失败: " + errorMsg);
            }

            // 下载并保存模型文件
            String baseName = description != null ? description : "image_model";
            String modelPath = downloadAndSaveModel(queryResponse, format, baseName);
            
            progressCallback.accept(100);
            logger.info("腾讯混元图片生成3D模型完成: {}", modelPath);
            return modelPath;

        } catch (Exception e) {
            logger.error("腾讯混元API调用失败", e);
            throw new Exception("腾讯混元3D模型生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePreviewImage(String modelPath) throws Exception {
        logger.info("生成模型预览图: {}", modelPath);
        
        // 创建预览目录
        Path previewDirPath = Paths.get(previewDir);
        if (!Files.exists(previewDirPath)) {
            Files.createDirectories(previewDirPath);
        }
        
        // 生成预览图文件名
        String filename = "preview_" + UUID.randomUUID().toString() + ".png";
        Path previewPath = previewDirPath.resolve(filename);
        
        // 创建简单的预览图（实际项目中可以集成3D渲染引擎）
        byte[] mockPngData = createMockPngData();
        Files.write(previewPath, mockPngData);
        
        logger.info("模型预览图生成完成: {}", previewPath.toString());
        return previewPath.toString();
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            return tencentAi3dClient != null && tencentAi3dClient.isServiceAvailable();
        } catch (Exception e) {
            logger.error("检查腾讯混元服务可用性失败", e);
            return false;
        }
    }

    /**
     * 轮询任务状态并更新进度
     */
    private QueryHunyuanTo3DJobResponse pollJobWithProgress(String jobId, int maxWaitTimeSeconds, 
                                                          int pollIntervalSeconds, 
                                                          Consumer<Integer> progressCallback) throws Exception {
        
        int totalPolls = maxWaitTimeSeconds / pollIntervalSeconds;
        int currentPoll = 0;
        
        while (currentPoll < totalPolls) {
            Thread.sleep(pollIntervalSeconds * 1000);
            currentPoll++;
            
            QueryHunyuanTo3DJobResponse response = tencentAi3dClient.queryHunyuanTo3DJob(jobId);
            
            if (response != null) {
                String status = response.getStatus();
                logger.debug("任务状态查询 - JobId: {}, Status: {}, Poll: {}/{}", 
                           jobId, status, currentPoll, totalPolls);
                
                // 计算进度 (25% - 90%)
                int progress = 25 + (int) ((double) currentPoll / totalPolls * 65);
                progressCallback.accept(Math.min(progress, 90));
                
                if (response.isCompleted()) {
                    progressCallback.accept(95);
                    return response;
                } else if (response.isFailed()) {
                    throw new RuntimeException("任务执行失败: " + response.getErrorMessage());
                }
                // 继续轮询其他状态（RUN, WAIT等）
            }
        }
        
        throw new RuntimeException("任务执行超时，JobId: " + jobId);
    }

    /**
     * 下载并保存模型文件
     */
    private String downloadAndSaveModel(QueryHunyuanTo3DJobResponse response, 
                                      ModelTask.OutputFormat format, 
                                      String baseName) throws IOException {
        
        // 创建模型目录
        Path modelDirPath = Paths.get(modelDir);
        if (!Files.exists(modelDirPath)) {
            Files.createDirectories(modelDirPath);
        }
        
        // 生成文件名
        String filename = "model_" + UUID.randomUUID().toString() + "." + format.name().toLowerCase();
        Path modelPath = modelDirPath.resolve(filename);
        
        // 从响应中获取模型数据
        String modelData;
        try {
            modelData = getModelDataFromResponse(response, format);
        } catch (IOException e) {
            logger.error("获取模型数据失败", e);
            throw new IOException("获取模型数据失败: " + e.getMessage(), e);
        }
        
        if (StringUtils.hasText(modelData)) {
            // 如果是Base64编码的数据，需要解码
            if (isBase64Encoded(modelData)) {
                byte[] decodedData = Base64.getDecoder().decode(modelData);
                Files.write(modelPath, decodedData);
            } else {
                // 直接保存文本数据
                Files.write(modelPath, modelData.getBytes());
            }
        } else {
            // 如果没有模型数据，创建一个占位文件
            String placeholderContent = "# 腾讯混元生成的3D模型\n# 模型名称: " + baseName + "\n# 格式: " + format;
            Files.write(modelPath, placeholderContent.getBytes());
        }
        
        return modelPath.toString();
    }

    /**
     * 从响应中获取模型数据
     */
    private String getModelDataFromResponse(QueryHunyuanTo3DJobResponse response, ModelTask.OutputFormat format) throws IOException {
        // 根据格式获取相应的模型数据
        if (response.getResultFile3Ds() != null && !response.getResultFile3Ds().isEmpty()) {
            // 查找匹配格式的文件
            String targetFormat = format.name();
            for (var file3D : response.getResultFile3Ds()) {
                if (file3D.getType() != null && file3D.getType().equalsIgnoreCase(targetFormat)) {
                    // 找到匹配格式的文件，下载文件内容
                    return downloadFileFromUrl(file3D.getUrl());
                }
            }
            
            // 如果没有找到匹配格式的文件，使用第一个文件
            if (!response.getResultFile3Ds().isEmpty()) {
                var firstFile = response.getResultFile3Ds().get(0);
                logger.warn("未找到格式为 {} 的文件，使用第一个文件: {}", targetFormat, firstFile.getType());
                return downloadFileFromUrl(firstFile.getUrl());
            }
        }
        
        return null;
    }
    
    /**
     * 从URL下载文件内容
     */
    private String downloadFileFromUrl(String fileUrl) throws IOException {
        if (!StringUtils.hasText(fileUrl)) {
            throw new IllegalArgumentException("文件URL不能为空");
        }
        
        logger.info("开始从URL下载文件: {}", fileUrl);
        
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            
            // 设置User-Agent，避免某些服务器拒绝请求
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    // 使用ByteArrayOutputStream替代readAllBytes()以避免过时API警告
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] data = new byte[1024];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    byte[] fileData = buffer.toByteArray();
                    logger.info("文件下载成功，大小: {} bytes", fileData.length);
                    
                    // 将文件数据转换为Base64编码返回
                    return Base64.getEncoder().encodeToString(fileData);
                }
            } else {
                throw new IOException("下载文件失败，HTTP响应码: " + responseCode);
            }
        } catch (Exception e) {
            logger.error("从URL下载文件失败: {}", fileUrl, e);
            throw new IOException("下载文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将图片转换为Base64编码
     */
    private String convertImageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        if (!Files.exists(path)) {
            throw new IOException("图片文件不存在: " + imagePath);
        }
        
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 转换输出格式为腾讯云支持的格式
     */
    private String convertToTencentFormat(ModelTask.OutputFormat format) {
        switch (format) {
            case OBJ:
                return "OBJ";
            case PLY:
                return "PLY";
            case STL:
                return "STL";
            case GLB:
                return "GLB";
            case USDZ:
                return "USDZ";
            case FBX:
                return "FBX";
            case MP4:
                return "MP4";
            default:
                return "OBJ"; // 默认使用OBJ格式
        }
    }

    /**
     * 检查字符串是否为Base64编码
     */
    private boolean isBase64Encoded(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 创建模拟PNG数据
     */
    private byte[] createMockPngData() {
        // 简单的PNG文件头和最小数据
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // width: 1
            0x00, 0x00, 0x00, 0x01, // height: 1
            0x08, 0x02, 0x00, 0x00, 0x00, // bit depth, color type, compression, filter, interlace
            (byte) 0x90, (byte) 0x77, (byte) 0x53, (byte) 0xDE, // CRC
            0x00, 0x00, 0x00, 0x0C, // IDAT chunk length
            0x49, 0x44, 0x41, 0x54, // IDAT
            0x08, (byte) 0x99, 0x01, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, // data
            0x00, 0x00, 0x00, 0x00, // IEND chunk length
            0x49, 0x45, 0x4E, 0x44, // IEND
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82 // CRC
        };
    }
}