package com.ping.pingpicture.domain.user.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 会员是否兑换枚举
 */
@Getter
public enum UserVipHasUsedEnum {

    USED("已使用", 1),
    UNUSED("未使用", 0);

    private final String text;

    private final int value;

    UserVipHasUsedEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static UserVipHasUsedEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserVipHasUsedEnum userVipHasUsedEnum : UserVipHasUsedEnum.values()) {
            if (userVipHasUsedEnum.value == value) {
                return userVipHasUsedEnum;
            }
        }
        return null;
    }
}
