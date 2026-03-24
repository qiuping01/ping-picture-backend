package com.ping.pingpicture.infrastructure.api.qwen.model;

import lombok.Data;

import java.util.List;

/**
 * AI 审核图片返回结果
 */
@Data
public class AuditImageResponse {

    /**
     * 是否正常
     */
    private String isNormal;

    /**
     * 是否危险
     */
    private String isDangerous;

    /**
     * 不通过原因
     */
    private String noPassReason;

    /**
     * 是否通过
     */
    private String isPass;

    /**
     * 描述
     */
    private String description;

    /**
     * 签列表
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;
}
