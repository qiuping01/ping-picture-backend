package com.ping.pingpicture.infrastructure.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.ping.pingpicture.infrastructure.manager.auth.model.SpaceUserAuthConfig;
import com.ping.pingpicture.infrastructure.manager.auth.model.SpaceUserRole;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.space.valueobject.SpaceRoleEnum;
import com.ping.pingpicture.domain.space.valueobject.SpaceTypeEnum;
import com.ping.pingpicture.application.service.SpaceUserApplicationService;
import com.ping.pingpicture.application.service.UserApplicationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 加载配置文件到对象
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (spaceUserRole == null) {
            return Collections.emptyList();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return Collections.emptyList();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表（SpaceVO、PictureVO） - 给前端使用
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return Collections.emptyList();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (loginUser.isAdmin()) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.emptyList();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return Collections.emptyList();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || loginUser.isAdmin()) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return Collections.emptyList();
                }
            case TEAM:
                // 团队空间，根据角色获取权限
                SpaceUser spaceUser = spaceUserApplicationService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return Collections.emptyList();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return Collections.emptyList();
    }
}