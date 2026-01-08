package com.ping.pingpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // 表示 lombok 会生成接收所有参数的构造器
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}