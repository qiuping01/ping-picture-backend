package com.ping.pingpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.ping.pingpicturebackend.manager.auth.SpaceUserAuthManager;
import com.ping.pingpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.ping.pingpicturebackend.model.entity.Picture;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.SpaceTypeEnum;
import com.ping.pingpicturebackend.service.PictureService;
import com.ping.pingpicturebackend.service.SpaceService;
import com.ping.pingpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 拦截器，建立连接前要先校验，并指定会话属性
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 握手前校验，并指定会话属性
     *
     * @param request    握手请求
     * @param response   握手响应
     * @param wsHandler  将要处理连接的 WebSocket 处理器
     * @param attributes 会话属性 Map，握手成功后传递给 WebSocketSession
     * @return 是否允许建立连接
     */
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                                   @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        // 1. 获取 Get 请求参数
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String pictureId = servletRequest.getParameter("PictureId");
        if (StrUtil.isBlank(pictureId)) {
            log.error("WebSocket 连接失败，缺少 PictureId 参数，拒绝握手");
            return false;
        }
        // 2. 获取登录用户信息
        User loginUser = userService.getLoginUser(servletRequest);
        if (ObjUtil.isEmpty(loginUser)) {
            log.error("WebSocket 连接失败，未登录用户，拒绝握手");
            return false;
        }
        // 3. 校验用户是否有该图片的权限
        Picture picture = pictureService.getById(pictureId);
        if (ObjUtil.isEmpty(picture)) {
            log.error("WebSocket 连接失败，图片不存在，拒绝握手");
            return false;
        }
        // 3.1. 校验是否是团队空间
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId == null || spaceId <= 0) {
            log.error("WebSocket 连接失败，图片关联的空间ID非法（为空或非正数），pictureId: {}, spaceId: {}", pictureId, spaceId);
            return false;
        }
        space = spaceService.getById(spaceId);
        if (ObjUtil.isEmpty(space)) {
            log.error("WebSocket 连接失败，图片所属空间不存在，拒绝握手");
            return false;
        }
        if (!space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
            log.error("WebSocket 连接失败，图片所属空间不是团队空间，拒绝握手");
            return false;
        }
        // 3.2. 校验图片编辑权限
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
            log.error("WebSocket 连接失败，用户没有图片编辑权限，拒绝握手");
            return false;
        }
        // 4. 指定会话属性
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", Long.valueOf(pictureId)); // 记得转为 Long 类型
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                               @NotNull WebSocketHandler wsHandler, Exception exception) {
    }
}