package org.example.types.enums;

/**
 * 生成任务状态枚举
 */
public enum TaskStatus {
    
    PENDING("PENDING", "等待中"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CACHED("CACHED", "缓存命中");
    
    private final String code;
    private final String desc;
    
    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
}