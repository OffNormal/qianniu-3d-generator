package com.qiniu.model3d.service.impl;

import com.qiniu.model3d.config.TencentCloudConfig;
import com.qiniu.model3d.dto.*;
import com.qiniu.model3d.service.TencentAi3dClient;
import com.tencentcloudapi.ai3d.v20250513.Ai3dClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯云混元生3D客户端实现类
 * 封装腾讯云官方SDK调用，提供统一的接口
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
public class TencentAi3dClientImpl implements TencentAi3dClient {

    private static final Logger logger = LoggerFactory.getLogger(TencentAi3dClientImpl.class);

    @Autowired
    private TencentCloudConfig tencentCloudConfig;

    private Ai3dClient ai3dClient;

    /**
     * 初始化腾讯云AI3D客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 创建认证对象
            Credential credential = new Credential(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
            );

            // 创建HTTP配置
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
            httpProfile.setConnTimeout(tencentCloudConfig.getAi3d().getTimeout());
            httpProfile.setReadTimeout(tencentCloudConfig.getAi3d().getTimeout());

            // 创建客户端配置
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 初始化客户端
            this.ai3dClient = new Ai3dClient(credential, tencentCloudConfig.getRegion(), clientProfile);
            
            logger.info("腾讯云AI3D客户端初始化成功，区域: {}", tencentCloudConfig.getRegion());
        } catch (Exception e) {
            logger.error("腾讯云AI3D客户端初始化失败", e);
            throw new RuntimeException("腾讯云AI3D客户端初始化失败", e);
        }
    }

    @Override
    public SubmitHunyuanTo3DJobResponse submitHunyuanTo3DJob(SubmitHunyuanTo3DJobRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        try {
            // 创建官方SDK请求对象
            com.tencentcloudapi.ai3d.v20250513.models.SubmitHunyuanTo3DJobRequest sdkRequest = 
                new com.tencentcloudapi.ai3d.v20250513.models.SubmitHunyuanTo3DJobRequest();

            // 设置请求参数
            if (StringUtils.hasText(request.getPrompt())) {
                sdkRequest.setPrompt(request.getPrompt());
            }
            if (StringUtils.hasText(request.getImageBase64())) {
                sdkRequest.setImageBase64(request.getImageBase64());
            }
            if (StringUtils.hasText(request.getImageUrl())) {
                sdkRequest.setImageUrl(request.getImageUrl());
            }
            if (request.getMultiViewImages() != null && !request.getMultiViewImages().isEmpty()) {
                // 转换多视角图片
                com.tencentcloudapi.ai3d.v20250513.models.ViewImage[] sdkViewImages = 
                    new com.tencentcloudapi.ai3d.v20250513.models.ViewImage[request.getMultiViewImages().size()];
                for (int i = 0; i < request.getMultiViewImages().size(); i++) {
                    ViewImage viewImage = request.getMultiViewImages().get(i);
                    com.tencentcloudapi.ai3d.v20250513.models.ViewImage sdkViewImage = 
                        new com.tencentcloudapi.ai3d.v20250513.models.ViewImage();
                    // 使用反射或直接设置字段，因为官方SDK可能没有setter方法
                    try {
                        java.lang.reflect.Field viewField = sdkViewImage.getClass().getDeclaredField("View");
                        viewField.setAccessible(true);
                        viewField.set(sdkViewImage, viewImage.getView());
                        
                        if (StringUtils.hasText(viewImage.getImageBase64())) {
                            java.lang.reflect.Field imageBase64Field = sdkViewImage.getClass().getDeclaredField("ImageBase64");
                            imageBase64Field.setAccessible(true);
                            imageBase64Field.set(sdkViewImage, viewImage.getImageBase64());
                        }
                        if (StringUtils.hasText(viewImage.getImageUrl())) {
                            java.lang.reflect.Field imageUrlField = sdkViewImage.getClass().getDeclaredField("ImageUrl");
                            imageUrlField.setAccessible(true);
                            imageUrlField.set(sdkViewImage, viewImage.getImageUrl());
                        }
                    } catch (Exception e) {
                        logger.warn("设置ViewImage字段失败，跳过多视角图片: {}", e.getMessage());
                        continue;
                    }
                    sdkViewImages[i] = sdkViewImage;
                }
                sdkRequest.setMultiViewImages(sdkViewImages);
            }
            if (StringUtils.hasText(request.getResultFormat())) {
                sdkRequest.setResultFormat(request.getResultFormat());
            }
            if (request.getEnablePBR() != null) {
                sdkRequest.setEnablePBR(request.getEnablePBR());
            }

            // 调用官方SDK
            com.tencentcloudapi.ai3d.v20250513.models.SubmitHunyuanTo3DJobResponse sdkResponse = 
                ai3dClient.SubmitHunyuanTo3DJob(sdkRequest);

            // 转换响应
            SubmitHunyuanTo3DJobResponse response = new SubmitHunyuanTo3DJobResponse();
            response.setJobId(sdkResponse.getJobId());
            response.setRequestId(sdkResponse.getRequestId());

            logger.info("提交混元生3D任务成功，任务ID: {}", response.getJobId());
            return response;

        } catch (TencentCloudSDKException e) {
            logger.error("提交混元生3D任务失败: {}", e.getMessage(), e);
            throw new Exception("提交混元生3D任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryHunyuanTo3DJobResponse queryHunyuanTo3DJob(QueryHunyuanTo3DJobRequest request) throws Exception {
        if (request == null || !StringUtils.hasText(request.getJobId())) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        try {
            // 创建官方SDK请求对象
            com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DJobRequest sdkRequest = 
                new com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DJobRequest();
            sdkRequest.setJobId(request.getJobId());

            // 调用官方SDK
            com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DJobResponse sdkResponse = 
                ai3dClient.QueryHunyuanTo3DJob(sdkRequest);

            // 转换响应
            QueryHunyuanTo3DJobResponse response = new QueryHunyuanTo3DJobResponse();
            response.setStatus(sdkResponse.getStatus());
            response.setErrorCode(sdkResponse.getErrorCode());
            response.setErrorMessage(sdkResponse.getErrorMessage());
            response.setRequestId(sdkResponse.getRequestId());

            // 转换结果文件
            if (sdkResponse.getResultFile3Ds() != null && sdkResponse.getResultFile3Ds().length > 0) {
                List<File3D> resultFiles = new ArrayList<>();
                for (com.tencentcloudapi.ai3d.v20250513.models.File3D sdkFile : sdkResponse.getResultFile3Ds()) {
                    File3D file3D = new File3D();
                    file3D.setType(sdkFile.getType());
                    file3D.setUrl(sdkFile.getUrl());
                    file3D.setPreviewImageUrl(sdkFile.getPreviewImageUrl());
                    resultFiles.add(file3D);
                }
                response.setResultFile3Ds(resultFiles);
            }

            logger.debug("查询混元生3D任务状态: 任务ID={}, 状态={}", request.getJobId(), response.getStatus());
            return response;

        } catch (TencentCloudSDKException e) {
            logger.error("查询混元生3D任务失败: {}", e.getMessage(), e);
            throw new Exception("查询混元生3D任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryHunyuanTo3DJobResponse queryHunyuanTo3DJob(String jobId) throws Exception {
        QueryHunyuanTo3DJobRequest request = new QueryHunyuanTo3DJobRequest();
        request.setJobId(jobId);
        return queryHunyuanTo3DJob(request);
    }

    @Override
    public SubmitHunyuanTo3DJobResponse submitTextTo3DJob(String prompt, String resultFormat, Boolean enablePBR) throws Exception {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("文本描述不能为空");
        }

        SubmitHunyuanTo3DJobRequest request = new SubmitHunyuanTo3DJobRequest();
        request.setPrompt(prompt);
        if (StringUtils.hasText(resultFormat)) {
            request.setResultFormat(resultFormat);
        }
        if (enablePBR != null) {
            request.setEnablePBR(enablePBR);
        }

        return submitHunyuanTo3DJob(request);
    }

    @Override
    public SubmitHunyuanTo3DJobResponse submitImageUrlTo3DJob(String imageUrl, String resultFormat, Boolean enablePBR) throws Exception {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("图片URL不能为空");
        }

        SubmitHunyuanTo3DJobRequest request = new SubmitHunyuanTo3DJobRequest();
        request.setImageUrl(imageUrl);
        if (StringUtils.hasText(resultFormat)) {
            request.setResultFormat(resultFormat);
        }
        if (enablePBR != null) {
            request.setEnablePBR(enablePBR);
        }

        return submitHunyuanTo3DJob(request);
    }

    @Override
    public SubmitHunyuanTo3DJobResponse submitImageBase64To3DJob(String imageBase64, String resultFormat, Boolean enablePBR) throws Exception {
        if (!StringUtils.hasText(imageBase64)) {
            throw new IllegalArgumentException("图片Base64数据不能为空");
        }

        SubmitHunyuanTo3DJobRequest request = new SubmitHunyuanTo3DJobRequest();
        request.setImageBase64(imageBase64);
        if (StringUtils.hasText(resultFormat)) {
            request.setResultFormat(resultFormat);
        }
        if (enablePBR != null) {
            request.setEnablePBR(enablePBR);
        }

        return submitHunyuanTo3DJob(request);
    }

    @Override
    public QueryHunyuanTo3DJobResponse pollJobUntilComplete(String jobId, int maxWaitTimeSeconds, int pollIntervalSeconds) throws Exception {
        if (!StringUtils.hasText(jobId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (maxWaitTimeSeconds <= 0) {
            maxWaitTimeSeconds = 300; // 默认5分钟
        }
        if (pollIntervalSeconds <= 0) {
            pollIntervalSeconds = 5; // 默认5秒
        }

        long startTime = System.currentTimeMillis();
        long maxWaitTimeMs = maxWaitTimeSeconds * 1000L;

        logger.info("开始轮询任务状态，任务ID: {}, 最大等待时间: {}秒, 轮询间隔: {}秒", 
                   jobId, maxWaitTimeSeconds, pollIntervalSeconds);

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            QueryHunyuanTo3DJobResponse response = queryHunyuanTo3DJob(jobId);
            
            if (response.isCompleted()) {
                logger.info("任务完成，任务ID: {}", jobId);
                return response;
            } else if (response.isFailed()) {
                logger.error("任务失败，任务ID: {}, 错误码: {}, 错误信息: {}", 
                           jobId, response.getErrorCode(), response.getErrorMessage());
                return response;
            }

            // 等待下次轮询
            try {
                Thread.sleep(pollIntervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("轮询被中断", e);
            }
        }

        throw new Exception("任务轮询超时，任务ID: " + jobId);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // 通过查询一个不存在的任务来测试服务可用性
            queryHunyuanTo3DJob("test-availability-check");
            return true;
        } catch (Exception e) {
            // 如果是因为任务不存在而报错，说明服务是可用的
            if (e.getMessage() != null && 
                (e.getMessage().contains("JobId") || 
                 e.getMessage().contains("任务不存在") || 
                 e.getMessage().contains("Task not found"))) {
                logger.debug("AI3D服务可用 - 通过任务不存在错误确认: {}", e.getMessage());
                return true;
            }
            logger.warn("AI3D服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getClientInfo() {
        return String.format("TencentAi3dClient - Region: %s, Endpoint: ai3d.tencentcloudapi.com, Version: 2025-05-13", 
                           tencentCloudConfig.getRegion());
    }
}