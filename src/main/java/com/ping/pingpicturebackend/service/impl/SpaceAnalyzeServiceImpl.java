package com.ping.pingpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.exception.ThrowUtils;
import com.ping.pingpicturebackend.mapper.SpaceMapper;
import com.ping.pingpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.ping.pingpicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.ping.pingpicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.ping.pingpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.ping.pingpicturebackend.model.entity.Picture;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.ping.pingpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.ping.pingpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.ping.pingpicturebackend.service.PictureService;
import com.ping.pingpicturebackend.service.SpaceAnalyzeService;
import com.ping.pingpicturebackend.service.SpaceService;
import com.ping.pingpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 空间分析服务接口实现
 */
@Slf4j
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest 分析请求
     * @param loginUser                登录用户
     * @return 分析结果
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                          User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        // 分析空间
        if (queryAll || queryPublic) {
            // 查询全部空间或公共图库：仅系统管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            // 直接返回 Object 对象提高性能
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream()
                    .mapToLong(obj -> obj instanceof Long ? (Long) obj : 0)
                    .sum();
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            // 公共图库无上限、无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 指定空间分析权限校验：仅空间管理员可访问
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf((spaceId == null || spaceId <= 0), ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 仅空间管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 构造返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            // 计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类分析请求
     * @param loginUser                   登录用户
     * @return 分析结果数组
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                      User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 使用 MyBatis-Plus 分组查询
        queryWrapper.select("category",
                        "count(*) as count",
                        "sum(picSize) as totalSize")
                .groupBy("category");
        // 4. 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    // 计数和大小：确保非null，否则给0
                    Long count = Optional.ofNullable(result.get("count"))
                            .map(num -> ((Number) num).longValue())
                            .orElse(0L);
                    Long totalSize = Optional.ofNullable(result.get("totalSize"))
                            .map(num -> ((Number) num).longValue())
                            .orElse(0L);
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * 空间图片标签分析
     *
     * @param spaceTagAnalyzeRequest 空间标签分析请求
     * @param loginUser              登录用户
     * @return 分析结果数组
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);
        // 4. 查询所有符合条件的结果
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                // 进一步过滤空的 JSON 数组字符串 "[]" 或空字符串 ""
                .filter(tag -> !tag.trim().isEmpty())
                .filter(tag -> !"[]".equals(tag.trim()))
                .collect(Collectors.toList());
        // 5. 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 6. 根据使用次数降序排序标签
        return tagCountMap.entrySet().stream()
                // 降序排列
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 根据分析范围填充查询对象
     *
     * @param spaceAnalyzeRequest 分析请求
     * @param queryWrapper        查询对象 - 图片
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                         QueryWrapper<Picture> queryWrapper) {
        // 全空间分析
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        // 公共图库分析
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        // 指定空间分析
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest 分析请求
     * @param loginUser           登录用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 全空间分析或者公共图库权限校验：仅管理员可访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        } else {
            // 指定空间分析权限校验：仅空间管理员可访问
            ThrowUtils.throwIf((spaceId == null || spaceId <= 0), ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
    }
}




