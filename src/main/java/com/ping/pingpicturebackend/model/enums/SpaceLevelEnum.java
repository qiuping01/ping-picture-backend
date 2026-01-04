package com.ping.pingpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {

    COMMON("普通版", 0, 100, 100 * 1024 * 1024L),
    PROFESSIONAL("专业版", 1, 1000, 1000 * 1024 * 1024L),
    ULTRA("旗舰版", 2, 10000, 10000 * 1024 * 1024L),
    ;

    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;

    /**
     * @param text     文本
     * @param value    值
     * @param maxCount 最大图片总数量
     * @param maxSize  最大图片总大小
     */
    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据 value 获取枚举
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.value == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
