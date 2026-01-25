package com.ping.pingpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicturebackend.common.DeleteRequest;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.exception.ThrowUtils;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.SpaceUser;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.SpaceLevelEnum;
import com.ping.pingpicturebackend.model.enums.SpaceRoleEnum;
import com.ping.pingpicturebackend.model.enums.SpaceTypeEnum;
import com.ping.pingpicturebackend.model.vo.SpaceUserVO;
import com.ping.pingpicturebackend.model.vo.SpaceVO;
import com.ping.pingpicturebackend.model.vo.UserVO;
import com.ping.pingpicturebackend.service.SpaceService;
import com.ping.pingpicturebackend.service.SpaceUserService;
import com.ping.pingpicturebackend.mapper.SpaceUserMapper;
import com.ping.pingpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 21877
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2026-01-24 22:58:46
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 创建请求
     * @return 空间成员 id
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        // 操作数据库
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建失败");
        return spaceUser.getId();
    }

    /**
     * 验证空间成员
     *
     * @param spaceUser 空间成员
     * @param add       true: 创建 / false: 编辑
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        // 创建时，空间 id 和 用户 id 必填
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 编辑时，校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    /**
     * 构造查询 QueryWrapper
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    /**
     * 获取单个空间成员封装 - 单条
     *
     * @param spaceUser 空间成员
     * @return SpaceUserVO
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 获取列表空间成员封装 - 列表
     *
     * @param spaceUserList 空间成员列表
     * @return SpaceUserVO列表
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 列表脱敏
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 用户 id 集合
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        // 空间 id 集合
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        // 查询用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 查询空间信息
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 填充用户和空间信息到列表中
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(spaceService.getSpaceVO(space));
        });
        return spaceUserVOList;
    }

    /**
     * 编辑空间成员
     *
     * @param spaceUserEditRequest 编辑请求
     * @param loginUser            登录用户
     */
    @Override
    public void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        validSpaceUser(spaceUser,false);
        // 判断该条关系是否存在 - 查库操作往后放
        SpaceUser oldSpaceUser = this.getById(spaceUserEditRequest.getId());
        if (oldSpaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 操作数据库
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest, User loginUser) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断该条关系是否存在 - 查库操作往后放
        SpaceUser oldSpaceUser = this.getById(deleteRequest.getId());
        if (oldSpaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 操作数据库
        boolean result = this.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
}




