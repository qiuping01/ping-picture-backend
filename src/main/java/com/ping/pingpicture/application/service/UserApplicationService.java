package com.ping.pingpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.interfaces.dto.user.UserLoginRequest;
import com.ping.pingpicture.interfaces.dto.user.UserQueryRequest;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.interfaces.dto.user.UserRegisterRequest;
import com.ping.pingpicture.interfaces.vo.user.LoginUserVO;
import com.ping.pingpicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author 21877
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-10-30 22:06:44
 */
public interface UserApplicationService {

    /**
     * 用户注册
     *
     * @return 用户ID
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          Http请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest,
                          HttpServletRequest request);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVoList(List<User> userList);

    long addUser(User user);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
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

    /**
     * 兑换会员码
     *
     * @param code      兑换码
     * @param loginUser 当前登录用户
     */
    void userExchangeVip(String code, User loginUser);
}

