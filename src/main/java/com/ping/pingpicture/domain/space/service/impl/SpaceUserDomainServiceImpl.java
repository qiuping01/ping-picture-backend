package com.ping.pingpicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.space.service.SpaceUserDomainService;
import com.ping.pingpicture.infrastructure.mapper.SpaceUserMapper;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import org.springframework.stereotype.Service;

/**
 * @author 21877
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2026-01-24 22:58:46
 */
@Service
public class SpaceUserDomainServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserDomainService {

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
}




