package com.ping.pingpicturebackend.service;

import com.ping.pingpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 21877
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-30 22:06:44
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码
     *
     * @param userPassword  用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
}
