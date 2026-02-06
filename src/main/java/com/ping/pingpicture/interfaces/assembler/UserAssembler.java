package com.ping.pingpicture.interfaces.assembler;

import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.interfaces.dto.user.UserAddRequest;
import com.ping.pingpicture.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 用户对象转换 - 转换器
 */
public class UserAssembler {

    public static User toUserEntity(UserAddRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}
