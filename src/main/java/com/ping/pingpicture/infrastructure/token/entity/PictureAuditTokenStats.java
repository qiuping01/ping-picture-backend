package com.ping.pingpicture.infrastructure.token.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 图片 AI 调用 token 统计表
 * @TableName picture_audit_token_stats
 */
@TableName(value ="picture_audit_token_stats")
@Data
public class PictureAuditTokenStats implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间id（公共图库为null）
     */
    private Long spaceId;

    /**
     * 任务类型：review/outpainting
     */
    private String taskType;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 输入token数
     */
    private Long inputTokens;

    /**
     * 输出token数
     */
    private Long outputTokens;

    /**
     * 总token数
     */
    private Long totalTokens;

    /**
     * 调用成本（元），精确到微元
     */
    private BigDecimal cost;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 调用耗时（毫秒）
     */
    private Long costTime;

    /**
     * 失败时的错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}