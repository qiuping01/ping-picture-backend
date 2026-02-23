package com.ping.pingpicture.domain.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员兑换码
 * @TableName vip_exchange_code
 */
@TableName(value ="vip_exchange_code")
@Data
public class VipExchangeCode implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换码
     */
    private String exchangeCode;

    /**
     * 是否已兑换
     */
    private Integer hasUsed;

    /**
     * 来源活动/渠道
     */
    private String source;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}