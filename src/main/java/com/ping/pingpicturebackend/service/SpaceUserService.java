package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.ping.pingpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.ping.pingpicturebackend.model.entity.SpaceUser;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.SpaceUserVO;

import java.util.List;

/**
 * @author 21877
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2026-01-24 22:58:46
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 创建请求
     * @return 空间成员 id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 验证空间成员
     *
     * @param spaceUser 空间成员
     * @param add       true: 创建 / false: 编辑
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 构造查询 QueryWrapper
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取单个空间成员封装 - 单条
     *
     * @param spaceUser 空间成员
     * @return SpaceUserVO
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取列表空间成员封装 - 列表
     *
     * @param spaceUserList 空间成员列表
     * @return SpaceUserVO列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 编辑空间成员
     *
     * @param spaceUserEditRequest 编辑请求
     * @param loginUser            登录用户
     */
    void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser);

    /**
     * 删除空间成员
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     */
    void deleteSpaceUser(DeleteRequest deleteRequest, User loginUser);
}
