package com.ping.pingpicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.ping.pingpicture.interfaces.vo.space.SpaceUserVO;

import java.util.List;

/**
 * @author 21877
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2026-01-24 22:58:46
 */
public interface SpaceUserDomainService extends IService<SpaceUser> {

    /**
     * 构造查询 QueryWrapper
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
