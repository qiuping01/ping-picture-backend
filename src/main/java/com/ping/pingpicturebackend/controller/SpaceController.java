package com.ping.pingpicturebackend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ping.pingpicturebackend.common.BaseResponse;
import com.ping.pingpicturebackend.common.DeleteRequest;
import com.ping.pingpicturebackend.common.ResultUtils;
import com.ping.pingpicturebackend.constant.UserConstant;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.exception.ThrowUtils;
import com.ping.pingpicturebackend.model.dto.space.SpaceAddRequest;
import com.ping.pingpicturebackend.model.dto.space.SpaceLevel;
import com.ping.pingpicturebackend.model.dto.space.SpaceQueryRequest;
import com.ping.pingpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.SpaceLevelEnum;
import com.ping.pingpicturebackend.model.vo.SpaceVO;
import com.ping.pingpicturebackend.service.SpaceService;
import com.ping.pingpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间接口
 */
@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    /**
     * 创建空间
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newSpaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newSpaceId);
    }

    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        spaceService.deleteSpace(deleteRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（仅管理员可用）
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                             HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(spaceUpdateRequest.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 空间校验
        spaceService.validSpace(space, true);
        // 填充空间级别参数
        spaceService.fillSpaceBySpaceLevel(space);
        // 更新空间
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）- 查询
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（封装类）- 查询
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查数据库
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 返回给前端所有的空间级别信息
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}
