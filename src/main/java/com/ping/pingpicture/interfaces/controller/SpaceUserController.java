package com.ping.pingpicture.interfaces.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.DeleteRequest;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.ping.pingpicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.ping.pingpicture.domain.space.entity.SpaceUser;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.interfaces.vo.space.SpaceUserVO;
import com.ping.pingpicture.application.service.SpaceUserApplicationService;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.shared.auth.model.SpaceUserPermissionConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员接口
 */
@Slf4j
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        spaceUserApplicationService.deleteSpaceUser(deleteRequest, null);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在空间的信息
     */
    @PostMapping("/get")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询空间成员列表
     */
    @PostMapping("/list")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息
     */
    @PostMapping("/edit")
    @SaCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null, ErrorCode.PARAMS_ERROR);
        spaceUserApplicationService.editSpaceUser(spaceUserEditRequest, null);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }
}