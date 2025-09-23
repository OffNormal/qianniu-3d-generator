package org.example.domain.generation.service;

import org.example.types.model.HunyuanRequest;
import org.example.types.model.HunyuanResponse;

/**
 * 腾讯混元3D服务接口
 */
public interface IHunyuanService {
    
    /**
     * 提交混元生3D任务
     * 
     * @param request 请求参数
     * @return 任务响应
     */
    HunyuanResponse submitHunyuanTo3DJob(HunyuanRequest request);
    
    /**
     * 查询任务状态
     * 
     * @param jobId 任务ID
     * @return 任务状态信息
     */
    Object queryJobStatus(String jobId);
    
    /**
     * 验证请求参数
     * 
     * @param request 请求参数
     * @throws IllegalArgumentException 参数验证失败时抛出
     */
    void validateRequest(HunyuanRequest request);
}