package com.ping.pingpicture.domain.user.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.space.repository.SpaceRepository;
import com.ping.pingpicture.domain.space.valueobject.SpaceLevelEnum;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private StringRedisTemplate stringRedisTemplate; // 用于生成VIP编号


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
//    private static long vipNumber = 0; 已改用 Redis 原子递增

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

        // 检查是否已经是VIP
        if (UserConstant.VIP_ROLE.equals(freshUser.getUserRole())) {
            // 如果已经是VIP，可以续期
            Date currentExpireTime = freshUser.getVipExpireTime();
            Date newExpireTime;
            if (currentExpireTime != null && currentExpireTime.after(new Date())) {
                // 在原有过期时间上加1年
                newExpireTime = DateUtil.offsetMonth(currentExpireTime, 12);
            } else {
                // 已过期，从当前时间加1年
                newExpireTime = DateUtil.offsetMonth(new Date(), 12);
            }
            freshUser.setVipExpireTime(newExpireTime);
        } else {
            // 新升级VIP
            Date expireTime = DateUtil.offsetMonth(new Date(), 12);
            freshUser.setUserRole(UserConstant.VIP_ROLE);
            freshUser.setVipCode(code.getExchangeCode());
            freshUser.setVipExpireTime(expireTime);

            // 生成VIP编号（使用Redis原子递增）
            long vipNumber = generateVipNumber();
            freshUser.setVipNumber(vipNumber);
        }

        // 更新用户
        boolean userUpdated = userRepository.updateById(freshUser);
        if (!userUpdated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户会员状态更新失败");
        }

        // 执行空间扩容
        upgradeUserSpaces(freshUser);
    }

    /**
     * 升级用户的所有空间
     */
    private void upgradeUserSpaces(User user) {
        // 1. 查询用户的所有空间
        List<Space> spaces = spaceRepository.lambdaQuery()
                .eq(Space::getUserId, user.getId())
                .list();

        if (spaces.isEmpty()) {
            // 2. 如果用户没有空间，创建默认空间
            createDefaultSpaceForVip(user);
        } else {
            // 3. 升级现有空间
            for (Space space : spaces) {
                upgradeSpace(space);
            }
        }
    }

    /**
     * 升级单个空间
     */
    private void upgradeSpace(Space space) {
        // 只升级普通空间（专业版以上不再升级）
        if (space.getSpaceLevel() == SpaceLevelEnum.COMMON.getValue()) {
            space.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());
            space.setMaxSize(SpaceLevelEnum.PROFESSIONAL.getMaxSize());
            space.setMaxCount(SpaceLevelEnum.PROFESSIONAL.getMaxCount());

            boolean updated = spaceRepository.updateById(space);
            if (!updated) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        String.format("空间扩容失败，空间ID: %d", space.getId()));
            }
        }
    }

    /**
     * 为VIP用户创建默认空间
     */
    private void createDefaultSpaceForVip(User user) {
        Space defaultSpace = new Space();
        defaultSpace.setUserId(user.getId());
        defaultSpace.setSpaceName("默认空间");
        defaultSpace.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());
        defaultSpace.setMaxSize(SpaceLevelEnum.PROFESSIONAL.getMaxSize());
        defaultSpace.setMaxCount(SpaceLevelEnum.PROFESSIONAL.getMaxCount());
        defaultSpace.setSpaceType(0);  // 0-私有空间
        defaultSpace.setCreateTime(new Date());

        boolean saved = spaceRepository.save(defaultSpace);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建默认空间失败");
        }
    }

    /**
     * 生成VIP编号（5位数，范围 10001-99999）
     */
    private long generateVipNumber() {
        String key = "vip:number";

        Long sequence = stringRedisTemplate.opsForValue().increment(key);

        if (sequence == null) {
            long fallback = System.currentTimeMillis() % 100000;
            return Math.max(fallback, 10001);
        }

        long vipNumber = sequence + 10000;  // 1→10001, 2→10002

        if (vipNumber > 99999) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "VIP编号已达上限，无法继续发放");
        }

        return vipNumber;
    }
}




