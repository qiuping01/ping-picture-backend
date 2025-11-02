package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ping.pingpicturebackend.model.dto.user.UserQueryRequest;
import com.ping.pingpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.vo.LoginUserVO;
import com.ping.pingpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     * 获得脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户
     */
    UserVO getUserVo(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVoList(List<User> userList);

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
     * @param request HTTP请求对象
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

    /**
     * 用户注销
     *
     * @param request HTTP请求对象
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 构建用户查询条件包装器
     * 将前端查询请求转换为MyBatis Plus的查询条件
     *
     * @param userQueryRequest 用户查询请求，包含查询条件和分页参数
     * @return MyBatis Plus查询条件包装器，用于数据库查询
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
