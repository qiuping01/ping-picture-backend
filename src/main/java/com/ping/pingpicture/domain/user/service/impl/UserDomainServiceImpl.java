package com.ping.pingpicture.domain.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.user.repository.UserRepository;
import com.ping.pingpicture.domain.user.service.UserDomainService;
import com.ping.pingpicture.domain.user.valueobject.UserRoleEnum;
import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import com.ping.pingpicture.infrastructure.common.UserNameGenerator;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.infrastructure.mapper.UserMapper;
import com.ping.pingpicture.interfaces.dto.user.UserQueryRequest;
import com.ping.pingpicture.interfaces.vo.user.LoginUserVO;
import com.ping.pingpicture.interfaces.vo.user.UserVO;
import com.ping.pingpicturebackend.manager.auth.satoken.DeviceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ping.pingpicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 21877
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-30 22:06:44
 */
@Slf4j
@Service
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserNameGenerator userNameGenerator;

    @Resource
    private UserRepository userRepository;

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
        // 2. 检查用户账户是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 密码一定要加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据到数据库中
        String uniqueUserName = userNameGenerator.generateUniqueUserName("图友");
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(uniqueUserName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = userRepository.save(user);
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
        User user = userRepository.getBaseMapper().selectOne(queryWrapper);
        // 不存在，抛异常
        if (user == null) {
            // 使用英文存储空间更小
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 保存用户的登录态
//        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // Sa-token 登录，并指定设备，同端登录互斥
        StpUtil.login(user.getId(), DeviceUtils.getRequestDevice(request));
        StpUtil.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
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
        Object loginUserId = StpUtil.getLoginIdDefaultNull();
        // 1. 先判断是否已经登录
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 从数据库查询（获取最新结果，追求的话性能上一步即可返回）
        User currentUser = userRepository.getById((String) loginUserId); // 数据库中的最新用户信息
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
        StpUtil.checkLogin();
        // 2. 移除登录态
        StpUtil.logout();
        return true;
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
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @param loginUser 当前登录用户
     * @return 如果当前用户是管理员，则返回true，否则返回false
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public long addUser(User user) {
        final String DEFAULT_PASSWORD = "12345678"; // 默认密码 12345678
        String encryptPassword = this.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public Boolean saveUser(User userEntity) {
        return userRepository.save(userEntity);
    }
}




