package com.ping.pingpicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员兑换码
 */
@Data
public class VipCode implements Serializable {

    private static final long serialVersionUID = 6841824394418996467L;

    /**
     * 兑换码
     */
    private String exchangeCode;

    /**
     * 是否已兑换 0 - 未兑换 1 - 已兑换
     */
    private Integer hasUsed;
}
