package com.ping.pingpicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ping.pingpicture.domain.picture.entity.Picture;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.space.repository.SpaceRepository;
import com.ping.pingpicture.domain.space.service.SpaceDomainService;
import com.ping.pingpicture.domain.space.valueobject.SpaceLevelEnum;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.infrastructure.mapper.PictureMapper;
import com.ping.pingpicture.interfaces.assembler.SpaceAssembler;
import com.ping.pingpicture.interfaces.dto.space.SpaceEditRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author 21877
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-03 19:53:40
 */
@Slf4j
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 构造空间查询 wrapper
     *
     * @param spaceQueryRequest 查询请求
     * @return 查询 wrapper
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest
                                                       spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        // 单字段搜索
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName)
                .eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel)
                .eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 填充空间级别参数
     *
     * @param space 空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null || space.getMaxSize() <= 0) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null || space.getMaxCount() <= 0) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 编辑请求
     * @param loginUser        登录用户
     */
    @Override
    public void editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
        Long spaceId = spaceEditRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceRepository.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 仅本人或管理员可编辑
        checkSpaceAuth(oldSpace, loginUser);
        // 编辑空间
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        // 数据校验 - 管理员操作校验
        space.validSpace(false);
        // 自动填充数据
        fillSpaceBySpaceLevel(space);
        space.setEditTime(new Date());
        // 操作数据库
        boolean result = spaceRepository.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑空间失败");
    }

    /**
     * 删除空间
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    @Override
    public void deleteSpace(DeleteRequest deleteRequest, User loginUser) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = deleteRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceRepository.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 仅本人或管理员可删除
        checkSpaceAuth(oldSpace, loginUser);
        // 删除空间 - 添加事务同时删除空间下的图片
        transactionTemplate.executeWithoutResult(status -> {
            boolean result = spaceRepository.removeById(spaceId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除空间失败");
            // 删除空间下的图片
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("spaceId", spaceId);
            int deletedCount = pictureMapper.delete(queryWrapper);
            log.info("删除空间时删除了 {} 张图片，spaceId: {}", deletedCount, spaceId);
        });
    }

    /**
     * 空间权限校验 - 仅本人或管理员可访问
     *
     * @param oldSpace  空间
     * @param loginUser 登录用户
     */
    @Override
    public void checkSpaceAuth(Space oldSpace, User loginUser) {
        // 仅本人或管理员可访问
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




