package com.ping.pingpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicturebackend.model.entity.SpaceUser;
import com.ping.pingpicturebackend.service.SpaceUserService;
import com.ping.pingpicturebackend.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 21877
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2026-01-24 22:58:46
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

}




