package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.ping.pingpicturebackend.model.entity.Space;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

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
}
