package com.ping.pingpicture.interfaces.controller;

import com.ping.pingpicture.application.service.CostStatisticsApplicationService;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.interfaces.dto.statistics.CostTrendRequest;
import com.ping.pingpicture.interfaces.dto.statistics.UserCostRankingRequest;
import com.ping.pingpicture.interfaces.vo.statistics.DailyCostTrendVO;
import com.ping.pingpicture.interfaces.vo.statistics.SpaceCostStatVO;
import com.ping.pingpicture.interfaces.vo.statistics.UserCostStatVO;
import com.ping.pingpicture.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * 成本统计接口
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private CostStatisticsApplicationService costStatisticsApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 获取今日总成本
     */
    @PostMapping("/cost/today")
    public BaseResponse<BigDecimal> getTodayCost(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        // 仅管理员可访问
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        BigDecimal todayTotalCost = costStatisticsApplicationService.getTodayTotalCost();
        return ResultUtils.success(todayTotalCost);
    }

    /**
     * 获取本月总成本
     */
    @PostMapping("/cost/month")
    public BaseResponse<BigDecimal> getMonthCost(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        BigDecimal monthTotalCost = costStatisticsApplicationService.getMonthTotalCost();
        return ResultUtils.success(monthTotalCost);
    }

    /**
     * 获取指定时间范围的总成本
     */
    @PostMapping("/cost/range")
    public BaseResponse<BigDecimal> getCostByRange(@RequestBody CostTrendRequest costTrendRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(costTrendRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");

        ThrowUtils.throwIf(costTrendRequest.getStartTime() == null, ErrorCode.PARAMS_ERROR, "开始时间不能为空");
        ThrowUtils.throwIf(costTrendRequest.getEndTime() == null, ErrorCode.PARAMS_ERROR, "结束时间不能为空");

        BigDecimal totalCost = costStatisticsApplicationService.getTotalCost(
                costTrendRequest.getStartTime(),
                costTrendRequest.getEndTime());
        return ResultUtils.success(totalCost);
    }

    /**
     * 获取用户成本排行
     */
    @PostMapping("/cost/user/top")
    public BaseResponse<List<UserCostStatVO>> getUserCostTop(@RequestBody UserCostRankingRequest userCostRankingRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(userCostRankingRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");

        Integer limit = userCostRankingRequest.getLimit();
        if (limit == null || limit <= 0) {
            limit = 10; // 默认前10
        }

        List<UserCostStatVO> resultList = costStatisticsApplicationService.getUserCostRanking(limit);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间成本排行（本月）
     */
    @PostMapping("/cost/space/list")
    public BaseResponse<List<SpaceCostStatVO>> getSpaceCostList(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        List<SpaceCostStatVO> resultList = costStatisticsApplicationService.getSpaceCostRanking();
        return ResultUtils.success(resultList);
    }

    /**
     * 获取近7天成本趋势
     */
    @PostMapping("/cost/trend")
    public BaseResponse<List<DailyCostTrendVO>> getCostTrend(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        List<DailyCostTrendVO> resultList =
                costStatisticsApplicationService.getLast7DaysTrend();

        return ResultUtils.success(resultList);
    }
}