package com.qiniu.model3d.service;

import com.qiniu.model3d.dto.QueryHunyuanTo3DJobRequest;
import com.qiniu.model3d.dto.QueryHunyuanTo3DJobResponse;
import com.qiniu.model3d.dto.SubmitHunyuanTo3DJobRequest;
import com.qiniu.model3d.dto.SubmitHunyuanTo3DJobResponse;

/**
 * 腾讯云混元生3D客户端接口
 * 基于混元大模型，根据输入的文本描述/图片智能生成3D
 * 封装腾讯云官方SDK调用
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
public interface TencentAi3dClient {

    /**
     * 提交混元生3D任务
     * 
     * @param request 提交任务请求参数
     * @return 提交任务响应，包含任务ID
     * @throws Exception 当API调用失败时抛出异常
     */
    SubmitHunyuanTo3DJobResponse submitHunyuanTo3DJob(SubmitHunyuanTo3DJobRequest request) throws Exception;

    /**
     * 查询混元生3D任务状态和结果
     * 
     * @param request 查询任务请求参数
     * @return 查询任务响应，包含任务状态和结果文件
     * @throws Exception 当API调用失败时抛出异常
     */
    QueryHunyuanTo3DJobResponse queryHunyuanTo3DJob(QueryHunyuanTo3DJobRequest request) throws Exception;

    /**
     * 根据任务ID查询任务状态和结果
     * 
     * @param jobId 任务ID
     * @return 查询任务响应，包含任务状态和结果文件
     * @throws Exception 当API调用失败时抛出异常
     */
    QueryHunyuanTo3DJobResponse queryHunyuanTo3DJob(String jobId) throws Exception;

    /**
     * 根据文本提示词提交3D生成任务
     * 
     * @param prompt 文本描述，最多支持1024个字符
     * @param resultFormat 生成模型的格式（可选）：OBJ、GLB、STL、USDZ、FBX、MP4
     * @param enablePBR 是否开启PBR材质生成（可选）
     * @return 提交任务响应，包含任务ID
     * @throws Exception 当API调用失败时抛出异常
     */
    SubmitHunyuanTo3DJobResponse submitTextTo3DJob(String prompt, String resultFormat, Boolean enablePBR) throws Exception;

    /**
     * 根据图片URL提交3D生成任务
     * 
     * @param imageUrl 图片URL
     * @param resultFormat 生成模型的格式（可选）：OBJ、GLB、STL、USDZ、FBX、MP4
     * @param enablePBR 是否开启PBR材质生成（可选）
     * @return 提交任务响应，包含任务ID
     * @throws Exception 当API调用失败时抛出异常
     */
    SubmitHunyuanTo3DJobResponse submitImageUrlTo3DJob(String imageUrl, String resultFormat, Boolean enablePBR) throws Exception;

    /**
     * 根据图片Base64数据提交3D生成任务
     * 
     * @param imageBase64 图片Base64编码数据
     * @param resultFormat 生成模型的格式（可选）：OBJ、GLB、STL、USDZ、FBX、MP4
     * @param enablePBR 是否开启PBR材质生成（可选）
     * @return 提交任务响应，包含任务ID
     * @throws Exception 当API调用失败时抛出异常
     */
    SubmitHunyuanTo3DJobResponse submitImageBase64To3DJob(String imageBase64, String resultFormat, Boolean enablePBR) throws Exception;

    /**
     * 轮询查询任务直到完成或失败
     * 
     * @param jobId 任务ID
     * @param maxWaitTimeSeconds 最大等待时间（秒）
     * @param pollIntervalSeconds 轮询间隔（秒）
     * @return 最终的任务查询响应
     * @throws Exception 当API调用失败或超时时抛出异常
     */
    QueryHunyuanTo3DJobResponse pollJobUntilComplete(String jobId, int maxWaitTimeSeconds, int pollIntervalSeconds) throws Exception;

    /**
     * 检查服务是否可用
     * 
     * @return true 如果服务可用，false 否则
     */
    boolean isServiceAvailable();

    /**
     * 获取客户端配置信息
     * 
     * @return 配置信息字符串
     */
    String getClientInfo();
}