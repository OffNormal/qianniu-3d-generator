package org.example.types.enums;

/**
 * 3D模型生成类型枚举
 */
public enum GenerationType {
    
    TEXT("TEXT", "文本转3D"),
    IMAGE("IMAGE", "图片转3D"),
    TEXT_TO_3D("TEXT_TO_3D", "文本转3D"),
    IMAGE_TO_3D("IMAGE_TO_3D", "图片转3D");
    
    private final String code;
    private final String desc;
    
    GenerationType(String code, String desc) {
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