package com.ping.pingpicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceAddRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceEditRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceQueryRequest;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.interfaces.vo.space.SpaceVO;

/**
 * @author 21877
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-01-03 19:53:40
 */
public interface SpaceApplicationService extends IService<Space> {

    /**
     * 添加空间
     *
     * @param spaceAddRequest 添加请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 构造查询 QueryWrapper
     *
     * @param spaceQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取单个空间封装
     *
     * @param space 空间
     * @return SpaceVO
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取分页空间封装
     *
     * @param spacePage 空间分页
     * @return SpaceVO分页
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 填充审核参数
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 编辑请求
     * @param loginUser        登录用户
     */
    void editSpace(SpaceEditRequest spaceEditRequest, User loginUser);

    /**
     * 删除空间
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    void deleteSpace(DeleteRequest deleteRequest, User loginUser);

    /**
     * 空间权限校验 - 仅本人或管理员可访问
     *
     * @param oldSpace 空间
     * @param loginUser 登录用户
     */
    void checkSpaceAuth(Space oldSpace, User loginUser);
}
