package com.ping.pingpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicturebackend.common.UserNameGenerator;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.UserRoleEnum;
import com.ping.pingpicturebackend.model.vo.LoginUserVO;
import com.ping.pingpicturebackend.service.UserService;
import com.ping.pingpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import static com.ping.pingpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 21877
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-30 22:06:44
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Autowired
    private UserNameGenerator userNameGenerator;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户ID
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword,checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查用户账户是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 密码一定要加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据到数据库中
        String uniqueUserName = userNameGenerator.generateUniqueUserName("无名用户");
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(uniqueUserName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      Http请求
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword,
                                 HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 对用户传递的密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 不存在，抛异常
        if (user == null) {
            // 使用英文存储空间更小
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 保存用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword  用户密码
     * @return 加密后的用户密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 1. 加盐,混淆密码
        final String SALT = "ping";
        // 2. 使用单向加密
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取当前的登录用户
     *
     * @param request HTTP请求对象
     * @return 登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 先判断是否已经登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 从数据库查询（获取最新结果，追求的话性能上一步即可返回）
        Long userId = currentUser.getId();
        currentUser = this.getById(userId); // 数据库中的最新用户信息
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null; // null 不转换，节省资源
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null ) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 2. 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }
}




