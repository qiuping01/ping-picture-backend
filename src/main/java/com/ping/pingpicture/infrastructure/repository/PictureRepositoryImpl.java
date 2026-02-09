package com.ping.pingpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.picture.entity.Picture;
import com.ping.pingpicture.domain.picture.repository.PictureRepository;
import com.ping.pingpicture.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓储实现
 */
@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
