package com.ping.pingpicture.application.service;

import com.ping.pingpicture.interfaces.vo.statistics.DailyCostTrendVO;
import com.ping.pingpicture.interfaces.vo.statistics.SpaceCostStatVO;
import com.ping.pingpicture.interfaces.vo.statistics.UserCostStatVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 成本统计应用服务接口
 */
public interface CostStatisticsApplicationService {

    // ========== 总成本统计 ==========

    /**
     * 获取今日总成本
     */
    BigDecimal getTodayTotalCost();

    /**
     * 获取本月总成本
     */
    BigDecimal getMonthTotalCost();

    /**
     * 获取指定时间范围的总成本
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    BigDecimal getTotalCost(Date startTime, Date endTime);

    // ========== 用户成本统计 ==========

    /**
     * 获取用户成本排行（本月，默认前10）
     */
    List<UserCostStatVO> getUserCostRanking();

    /**
     * 获取用户成本排行（指定时间范围）
     *
     * @param limit 限制条数
     */
    List<UserCostStatVO> getUserCostRanking(int limit);

    /**
     * 获取指定时间范围内的用户成本排行
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     限制条数
     */
    List<UserCostStatVO> getUserCostRanking(Date startTime, Date endTime, int limit);

    // ========== 空间成本统计 ==========

    /**
     * 获取空间成本排行（本月）
     */
    List<SpaceCostStatVO> getSpaceCostRanking();

    /**
     * 获取指定时间范围内的空间成本排行
     */
    List<SpaceCostStatVO> getSpaceCostRanking(Date startTime, Date endTime);

    // ========== 趋势分析 ==========

    /**
     * 获取近7天每日成本趋势
     */
    List<DailyCostTrendVO> getLast7DaysTrend();
}