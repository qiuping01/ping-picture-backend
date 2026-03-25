package com.ping.pingpicture.infrastructure.api.qwen.model;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通用状态枚举
 * 用于 isNormal、isDangerous、isPass 等字段
 */
@Getter
public enum AuditImageStatusEnum {

    YES("是", "yes"),
    NO("否", "no");

    private final String text;
    private final String value;

    AuditImageStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static AuditImageStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AuditImageStatusEnum auditImageStatusEnum : AuditImageStatusEnum.values()) {
            if (auditImageStatusEnum.value.equals(value)) {
                return auditImageStatusEnum;
            }
        }
        return null;
    }

    /**
     * 判断是否为"是"
     */
    public static boolean isYes(String value) {
        return YES.getValue().equals(value);
    }

    /**
     * 判断是否为"否"
     */
    public static boolean isNo(String value) {
        return NO.getValue().equals(value);
    }
    
    /**
     * 获取相反的枚举值
     */
    public AuditImageStatusEnum opposite() {
        return this == YES ? NO : YES;
    }
}