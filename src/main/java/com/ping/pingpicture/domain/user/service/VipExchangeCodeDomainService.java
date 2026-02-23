package com.ping.pingpicture.domain.user.service;

import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.user.entity.VipExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

public interface VipExchangeCodeDomainService extends IService<VipExchangeCode> {

    /**
     * 兑换会员码
     *
     * @param code      兑换码
     * @param loginUser 当前登录用户
     */
    void userExchangeVip(String code, User loginUser);
}
