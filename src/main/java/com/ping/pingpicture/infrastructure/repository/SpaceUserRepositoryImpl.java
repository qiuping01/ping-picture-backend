package com.ping.pingpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.space.repository.SpaceUserRepository;
import com.ping.pingpicture.infrastructure.mapper.SpaceUserMapper;

/**
 * 空间成员仓储
 */
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {

}
