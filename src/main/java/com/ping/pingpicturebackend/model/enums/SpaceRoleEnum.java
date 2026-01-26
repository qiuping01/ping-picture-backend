package com.ping.pingpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 团队空间角色枚举
 */
@Getter
public enum SpaceRoleEnum {

    ADMIN("管理员", "admin"),
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor");

    private final String text;

    private final String value;

    /**
     * @param text  文本
     * @param value 值
     */
    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.value.equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的值列表
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
