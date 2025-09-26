package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 提交混元生3D任务响应
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitHunyuanTo3DJobResponse {

    /**
     * 任务ID（有效期24小时）
     */
    private String jobId;

    /**
     * 唯一请求 ID，由服务端生成，每次请求都会返回
     * 定位问题时需要提供该次请求的 RequestId
     */
    private String requestId;

    public SubmitHunyuanTo3DJobResponse() {}

    public SubmitHunyuanTo3DJobResponse(String jobId, String requestId) {
        this.jobId = jobId;
        this.requestId = requestId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "SubmitHunyuanTo3DJobResponse{" +
                "jobId='" + jobId + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}