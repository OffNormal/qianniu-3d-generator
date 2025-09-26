package com.qiniu.model3d.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 查询混元生3D任务响应
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryHunyuanTo3DJobResponse {

    /**
     * 任务状态
     * WAIT：等待中，RUN：执行中，FAIL：任务失败，DONE：任务成功
     */
    private String status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 生成的3D文件数组
     */
    private List<File3D> resultFile3Ds;

    /**
     * 唯一请求 ID，由服务端生成，每次请求都会返回
     * 定位问题时需要提供该次请求的 RequestId
     */
    private String requestId;

    public QueryHunyuanTo3DJobResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<File3D> getResultFile3Ds() {
        return resultFile3Ds;
    }

    public void setResultFile3Ds(List<File3D> resultFile3Ds) {
        this.resultFile3Ds = resultFile3Ds;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * 判断任务是否完成
     */
    public boolean isCompleted() {
        return "DONE".equals(status);
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed() {
        return "FAIL".equals(status);
    }

    /**
     * 判断任务是否正在运行
     */
    public boolean isRunning() {
        return "RUN".equals(status);
    }

    /**
     * 判断任务是否等待中
     */
    public boolean isWaiting() {
        return "WAIT".equals(status);
    }

    @Override
    public String toString() {
        return "QueryHunyuanTo3DJobResponse{" +
                "status='" + status + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", resultFile3Ds=" + resultFile3Ds +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}