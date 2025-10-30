package com.ping.pingpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.service.UserService;
import com.ping.pingpicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 21877
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-30 22:06:44
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




