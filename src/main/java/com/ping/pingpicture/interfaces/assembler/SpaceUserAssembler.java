package com.ping.pingpicture.interfaces.assembler;

import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间成员对象转换 - 转换器
 */
public class SpaceUserAssembler {

    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request){
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceUserEntity(SpaceUserEditRequest request){
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}
