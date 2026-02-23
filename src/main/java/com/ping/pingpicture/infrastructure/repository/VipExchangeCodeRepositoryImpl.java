package com.ping.pingpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.user.entity.VipExchangeCode;
import com.ping.pingpicture.domain.user.repository.VipExchangeCodeRepository;
import com.ping.pingpicture.infrastructure.mapper.VipExchangeCodeMapper;
import org.springframework.stereotype.Service;

/**
 * 会员兑换码仓储实现
 */
@Service
public class VipExchangeCodeRepositoryImpl extends ServiceImpl<VipExchangeCodeMapper, VipExchangeCode> implements VipExchangeCodeRepository {
}
