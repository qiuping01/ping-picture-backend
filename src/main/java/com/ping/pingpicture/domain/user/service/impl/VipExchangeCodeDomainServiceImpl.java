package com.ping.pingpicture.domain.user.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.user.constant.UserConstant;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.user.entity.VipExchangeCode;
import com.ping.pingpicture.domain.user.repository.UserRepository;
import com.ping.pingpicture.domain.user.repository.VipExchangeCodeRepository;
import com.ping.pingpicture.domain.user.service.VipExchangeCodeDomainService;
import com.ping.pingpicture.domain.user.valueobject.UserVipHasUsedEnum;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.mapper.VipExchangeCodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class VipExchangeCodeDomainServiceImpl extends ServiceImpl<VipExchangeCodeMapper, VipExchangeCode>
    implements VipExchangeCodeDomainService {

    @Resource
    private VipExchangeCodeRepository vipExchangeCodeRepository;

    @Resource
    private UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userExchangeVip(String code, User loginUser) {
        // 1. 参数校验（不加锁）
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码不能为空");
        }

        // 2. 使用兑换码作为锁对象（细粒度锁）
        synchronized (code.intern()) {  // intern() 保证相同字符串使用同一个锁
            // 3. 查询并校验兑换码
            VipExchangeCode vipExchangeCode = vipExchangeCodeRepository.lambdaQuery()
                    .eq(VipExchangeCode::getExchangeCode, code)
                    .one();

            // 4. 各种校验
            if (vipExchangeCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码不存在");
            }
            if (vipExchangeCode.getHasUsed() == UserVipHasUsedEnum.USED.getValue()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已被使用");
            }

            // 5. 执行兑换（在锁内完成）
            executeExchange(vipExchangeCode, loginUser);
        }
    }

    // 在类里面加一个静态变量
    private static long vipNumber = 0;

    private void executeExchange(VipExchangeCode code, User user) {
        // 更新兑换码状态
        code.setHasUsed(UserVipHasUsedEnum.USED.getValue());

        boolean codeUpdated = vipExchangeCodeRepository.updateById(code);
        if (!codeUpdated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码更新失败");
        }

        // 重新查询用户（避免脏数据）
        User freshUser = userRepository.getById(user.getId());
        if (freshUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 更新用户
        // 计算过期时间（当前时间 + 1 年）
        Date expireTime = DateUtil.offsetMonth(new Date(), 12); // 计算当前时间加 1 年后的时间
        freshUser.setUserRole(UserConstant.VIP_ROLE);
        freshUser.setVipCode(code.getExchangeCode());
        freshUser.setVipExpireTime(expireTime);
        // 最简单的加一
        vipNumber++;
        freshUser.setVipNumber(vipNumber);

        boolean userUpdated = userRepository.updateById(freshUser);
        if (!userUpdated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户会员状态更新失败");
        }
    }
}




