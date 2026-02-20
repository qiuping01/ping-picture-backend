package com.ping.pingpicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.application.service.SpaceApplicationService;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.space.service.SpaceDomainService;
import com.ping.pingpicture.domain.space.valueobject.SpaceLevelEnum;
import com.ping.pingpicture.domain.space.valueobject.SpaceRoleEnum;
import com.ping.pingpicture.domain.space.valueobject.SpaceTypeEnum;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.infrastructure.mapper.SpaceMapper;
import com.ping.pingpicture.infrastructure.mapper.SpaceUserMapper;
import com.ping.pingpicture.interfaces.assembler.SpaceAssembler;
import com.ping.pingpicture.interfaces.dto.space.SpaceAddRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceEditRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceQueryRequest;
import com.ping.pingpicture.interfaces.vo.space.SpaceVO;
import com.ping.pingpicture.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 21877
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-03 19:53:40
 */
@Slf4j
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserMapper spaceUserMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 添加空间
     *
     * @param spaceAddRequest 添加请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        // DTO 处理完后再拷贝到实体
        Space space = SpaceAssembler.toSpaceEntity(spaceAddRequest);
        space.fillDefaultSpace();
        // 填充级别参数和用户 id
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 2. 校验参数
        space.validSpace(true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建高级别空间");
        }
        // 4. 控制同一用户只能创建一个同一类型的空间
        // 针对用户加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            // 加事务
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 查询用户是否已经存在同一类型的空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建失败");
                // 如果创建的是团队空间，关联新增团队成员记录
                Integer spaceType = space.getSpaceType();
                SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
                if (spaceTypeEnum == SpaceTypeEnum.TEAM) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    int insertResult = spaceUserMapper.insert(spaceUser);
                    ThrowUtils.throwIf(insertResult == 0, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 返回新写入的空间 id
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }


    /**
     * 构造空间查询 wrapper
     *
     * @param spaceQueryRequest 查询请求
     * @return 查询 wrapper
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
       return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    /**
     * 获取单个空间封装
     *
     * @param space 空间
     * @return SpaceVO
     */
    @Override
    public SpaceVO getSpaceVO(Space space) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        if (spaceVO == null) {
            return null;
        }
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取分页空间封装
     *
     * @param spacePage 空间分页
     * @return SpaceVO分页
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        // 拿到当前页数据
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>
                (spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 1. 转换为VO
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 2. 提取不重复的userId（Set去重）
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .filter(Objects::nonNull)  // 过滤null值
                .collect(Collectors.toSet());
        // 3. 批量查询用户
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 4. 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userApplicationService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 填充空间级别参数
     *
     * @param space 空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 编辑请求
     * @param loginUser        登录用户
     */
    @Override
    public void editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
        spaceDomainService.editSpace(spaceEditRequest, loginUser);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    @Override
    public void deleteSpace(DeleteRequest deleteRequest, User loginUser) {
       spaceDomainService.deleteSpace(deleteRequest, loginUser);
    }

    /**
     * 空间权限校验 - 仅本人或管理员可访问
     *
     * @param oldSpace  空间
     * @param loginUser 登录用户
     */
    @Override
    public void checkSpaceAuth(Space oldSpace, User loginUser) {
        spaceDomainService.checkSpaceAuth(oldSpace, loginUser);
    }
}




