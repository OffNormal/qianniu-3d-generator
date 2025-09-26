package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.NotBlank;

/**
 * 查询混元生3D任务请求
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryHunyuanTo3DJobRequest {

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String jobId;

    public QueryHunyuanTo3DJobRequest() {}

    public QueryHunyuanTo3DJobRequest(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        return "QueryHunyuanTo3DJobRequest{" +
                "jobId='" + jobId + '\'' +
                '}';
    }
}