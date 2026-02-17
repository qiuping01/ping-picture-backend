package com.ping.pingpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.space.repository.SpaceRepository;
import com.ping.pingpicture.infrastructure.mapper.SpaceMapper;

/**
 * 空间仓储实现
 */
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {

}
