package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.dto.space.analyze.*;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
 * 空间分析服务接口
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest 分析请求
     * @param loginUser                登录用户
     * @return 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                   User loginUser);

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类分析请求
     * @param loginUser                   登录用户
     * @return 分析结果数组
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间图片标签分析
     *
     * @param spaceTagAnalyzeRequest 空间标签分析请求
     * @param loginUser              登录用户
     * @return 分析结果数组
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 根据空间图片大小范围统计对应数量
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小分析请求
     * @param loginUser               登录用户
     * @return 分析结果数组
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户上传行为分析数据
     *
     * @param spaceUserAnalyzeRequest 用户上传行为分析请求
     * @param loginUser               登录用户
     * @return 分析结果
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间排行
     *
     * @param spaceRankAnalyzeRequest 空间排行分析请求
     * @param loginUser               登录用户
     * @return 分析结果
     */
    List<Space> getSpaceRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
