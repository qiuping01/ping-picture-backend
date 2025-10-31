package com.ping.pingpicturebackend.service;

import com.ping.pingpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

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
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      Http请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword,
                          HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword  用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前的登录用户
     *
     * @param request 会话请求
     * @return 登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user 当前用户
     * @return 脱敏后的用户
     */
    LoginUserVO getLoginUserVO(User user);
}
