package com.ping.pingpicturebackend.manager.auth.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.manager.auth.SpaceUserAuthContext;
import com.ping.pingpicturebackend.manager.auth.SpaceUserAuthManager;
import com.ping.pingpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.ping.pingpicturebackend.model.entity.Picture;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.SpaceUser;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.SpaceRoleEnum;
import com.ping.pingpicturebackend.model.enums.SpaceTypeEnum;
import com.ping.pingpicturebackend.service.PictureService;
import com.ping.pingpicturebackend.service.SpaceService;
import com.ping.pingpicturebackend.service.SpaceUserService;
import com.ping.pingpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;

import static com.ping.pingpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

@Component // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    private final SpaceUserAuthManager spaceUserAuthManager;

    public StpInterfaceImpl(SpaceUserAuthManager spaceUserAuthManager) {
        this.spaceUserAuthManager = spaceUserAuthManager;
    }

    /**
     * 返回一个账号所拥有的权限码集合
     * 注意：这个方法的 loginId 参数是当前登录用户的 ID（来自 StpUtil.login()）
     */
    @Override
    public List<String> getPermissionList(Object loginId, String s) {
        // 1. 获取管理员权限
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 2. 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 3. 如果所有字段都为空，表示查询公共图库，返回所有权限
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 4. 获取当前登录用户信息
        User loginUser = (User) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        // 5. 优先从上下文总获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 6. 没有 SpaceUser 对象，如果有 spaceUserId,必然是团队空间，直接查 SpaceUser对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间用户不存在");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .one();
            if (loginSpaceUser == null) {
                return Collections.emptyList();
            }
            // 由于这里查的是 space_user 表，只能拿到团队空间的权限，私有空间是没有管理员权限的
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 7. 如果没有 spaceUserId，则判断是否是私有空间，通过 spaceId 或 pictureId 获取 space 对象
        Long spaceId = authContext.getSpaceId();
        // 7.1. 如果 spaceId 为空，则通过 pictureId 获取 space 对象
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验，视为公共图库操作
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            // 通过 pictureId 获取 space 对象
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getSpaceId, Picture::getUserId, Picture::getId) // 减轻查库压力
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            spaceId = picture.getSpaceId();
            // 公共图库,仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }

        // 7.2. 如果 spaceId 不为空，直接获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                // 无权限返回空
                return Collections.emptyList();
            }
        } else {
            // 团队空间，通过 spaceUser 表获取权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
            if (spaceUser == null) {
                return Collections.emptyList();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String s) {
        // 从当前登录用户信息中获取角色
        User user = (User) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getUserRole());
    }

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        // 获取请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // 获取请求类别 get / post
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        // 对应封装结果
        SpaceUserAuthContext authRequest;
        if ((ContentType.JSON.getValue().equals(contentType))) {
            // post 请求
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            // get 请求
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            // 替换掉 api 上下文 "/api/"  → ""
            String pathURI = requestURI.replace(contextPath + "/", "");
            // 获取前缀的第一个斜杠前的字符串
            String moduleName = StrUtil.subBefore(pathURI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    /**
     * 判断所有字段都为空
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取对象的所有字段（包括私有字段）
        Field[] fields = ReflectUtil.getFields(object.getClass());
        // 用反射获取所有字段并判断是否所有字段都为空
        return Arrays.stream(fields)
                // 获取所有字段值
                .map(filed -> ReflectUtil.getFieldValue(object, filed))
                // 判断所有字段是否都为空
                .allMatch(ObjectUtil::isEmpty);
    }
}