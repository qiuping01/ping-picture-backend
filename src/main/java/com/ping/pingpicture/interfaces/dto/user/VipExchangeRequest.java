package com.ping.pingpicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员兑换请求
 */
@Data
public class VipExchangeRequest implements Serializable {

    private static final long serialVersionUID = 8777782236140609943L;

    /**
     * 兑换码
     */
    private String exchangeCode;
}
