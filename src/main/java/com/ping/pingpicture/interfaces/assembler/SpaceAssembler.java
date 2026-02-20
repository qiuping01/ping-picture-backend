package com.ping.pingpicture.interfaces.assembler;

import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.interfaces.dto.space.SpaceAddRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceEditRequest;
import com.ping.pingpicture.interfaces.dto.space.SpaceUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间对象转换 - 转换器
 */
public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest request){
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest request){
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request){
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}
