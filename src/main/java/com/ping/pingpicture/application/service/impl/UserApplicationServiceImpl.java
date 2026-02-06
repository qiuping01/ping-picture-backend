package com.ping.pingpicture.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.user.service.UserDomainService;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.infrastructure.common.UserNameGenerator;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.interfaces.dto.user.UserLoginRequest;
import com.ping.pingpicture.interfaces.dto.user.UserQueryRequest;
import com.ping.pingpicture.interfaces.dto.user.UserRegisterRequest;
import com.ping.pingpicture.interfaces.vo.user.LoginUserVO;
import com.ping.pingpicture.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author 21877
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-30 22:06:44
 */
@Slf4j
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserNameGenerator userNameGenerator;

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册
     *
     * @return 用户ID
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 1. 校验参数
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 2. 执行注册
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          Http请求
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest,
                                 HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 1. 校验
        User.validUserLogin(userAccount, userPassword);
        // 3. 查询数据库中的用户是否存在
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVoList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }

    /**
     * 获取当前的登录用户
     *
     * @param request HTTP请求对象
     * @return 登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    /**
     * 构建用户查询条件包装器
     * 将前端查询请求转换为MyBatis Plus的查询条件
     *
     * @param userQueryRequest 用户查询请求，包含查询条件和分页参数
     * @return MyBatis Plus查询条件包装器，用于数据库查询
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = new Page<>(current, size);
        QueryWrapper<User> queryWrapper = userDomainService.getQueryWrapper(userQueryRequest);
        Page<User> page = userDomainService.page(userPage, queryWrapper);
        Page<UserVO> userVOPage = new Page<>(current, size, page.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(page.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }
}
