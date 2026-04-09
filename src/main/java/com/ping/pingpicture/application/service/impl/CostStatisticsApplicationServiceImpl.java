package com.ping.pingpicture.application.service.impl;

import cn.hutool.core.date.DateUtil;
import com.ping.pingpicture.application.service.CostStatisticsApplicationService;
import com.ping.pingpicture.infrastructure.mapper.PictureAuditTokenStatsMapper;
import com.ping.pingpicture.interfaces.vo.statistics.DailyCostTrendVO;
import com.ping.pingpicture.interfaces.vo.statistics.SpaceCostStatVO;
import com.ping.pingpicture.interfaces.vo.statistics.UserCostStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本统计应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostStatisticsApplicationServiceImpl implements CostStatisticsApplicationService {

    private final PictureAuditTokenStatsMapper tokenStatsMapper;

    // ========== 总成本统计 ==========

    /**
     * 获取今日总成本
     */
    @Override
    public BigDecimal getTodayTotalCost() {
        Date startTime = DateUtil.beginOfDay(new Date());
        Date endTime = DateUtil.endOfDay(new Date());
        return getTotalCost(startTime, endTime);
    }

    /**
     * 获取本月总成本
     */
    @Override
    public BigDecimal getMonthTotalCost() {
        Date startTime = DateUtil.beginOfMonth(new Date());
        Date endTime = DateUtil.endOfMonth(new Date());
        return getTotalCost(startTime, endTime);
    }

    /**
     * 获取指定时间范围的总成本
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    @Override
    public BigDecimal getTotalCost(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal cost = tokenStatsMapper.sumCostByTimeRange(startTime, endTime);
        return cost != null ? cost : BigDecimal.ZERO;
    }

    // ========== 用户成本统计 ==========
    
    /**
     * 获取用户成本排行（本月，默认前10）
     */
    @Override
    public List<UserCostStatVO> getUserCostRanking() {
        return getUserCostRanking(10);
    }

    /**
     * 获取用户成本排行（指定时间范围）
     *
     * @param limit 限制条数
     */
    @Override
    public List<UserCostStatVO> getUserCostRanking(int limit) {
        Date startTime = DateUtil.beginOfMonth(new Date());
        Date endTime = DateUtil.endOfMonth(new Date());
        return getUserCostRanking(startTime, endTime, limit);
    }

    /**
     * 获取指定时间范围内的用户成本排行
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     限制条数
     */
    @Override
    public List<UserCostStatVO> getUserCostRanking(Date startTime, Date endTime, int limit) {
        // 参数校验
        if (startTime == null || endTime == null || limit <= 0) {
            return Collections.emptyList();
        }
        // 调用 Mapper 自定义方法
        List<Map<String, Object>> result = tokenStatsMapper.getUserCostStats(startTime, endTime, limit);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        // 转换为 VO
        return result.stream()
                .map(map -> {
                    Long userId = map.get("userId") != null ? ((Number) map.get("userId")).longValue() : null;
                    BigDecimal totalCost = map.get("totalCost") != null ? (BigDecimal) map.get("totalCost") : BigDecimal.ZERO;
                    Long callCount = map.get("callCount") != null ? ((Number) map.get("callCount")).longValue() : 0L;
                    return new UserCostStatVO(userId, totalCost, callCount);
                })
                .collect(Collectors.toList());
    }

    // ========== 空间成本统计 ==========

    /**
     * 获取空间成本排行（本月）
     */
    @Override
    public List<SpaceCostStatVO> getSpaceCostRanking() {
        Date startTime = DateUtil.beginOfMonth(new Date());
        Date endTime = DateUtil.endOfMonth(new Date());
        return getSpaceCostRanking(startTime, endTime);
    }

    /**
     * 获取指定时间范围内的空间成本排行
     */
    @Override
    public List<SpaceCostStatVO> getSpaceCostRanking(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = tokenStatsMapper.getSpaceCostStats(startTime, endTime);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream()
                .map(map -> {
                    Long spaceId = map.get("spaceId") != null ? ((Number) map.get("spaceId")).longValue() : null;
                    BigDecimal totalCost = map.get("totalCost") != null ? (BigDecimal) map.get("totalCost") : BigDecimal.ZERO;
                    Long callCount = map.get("callCount") != null ? ((Number) map.get("callCount")).longValue() : 0L;
                    return new SpaceCostStatVO(spaceId, totalCost, callCount);
                })
                .collect(Collectors.toList());
    }

    // ========== 趋势分析 ==========

    /**
     * 获取近7天每日成本趋势
     */
    @Override
    public List<DailyCostTrendVO> getLast7DaysTrend() {
        // 获取近7天的日期列表
        List<LocalDate> last7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            last7Days.add(LocalDate.now().minusDays(i));
        }

        // 查询数据库中的实际数据
        Date startTime = DateUtil.beginOfDay(DateUtil.yesterday()); // 从7天前开始
        Date endTime = DateUtil.endOfDay(new Date());
        List<Map<String, Object>> dbResult = tokenStatsMapper.getDailyTrend(startTime, endTime, 7);

        // 转换为 Map<日期, 数据>
        Map<LocalDate, DailyCostTrendVO> dataMap = new HashMap<>();
        if (dbResult != null) {
            for (Map<String, Object> row : dbResult) {
                // 根据 Mapper 返回的字段处理，假设返回 date 和 dailyCost, callCount
                Object dateObj = row.get("date");
                if (dateObj == null) continue;
                LocalDate date = null;
                if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof Date) {
                    date = ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                if (date != null) {
                    BigDecimal dailyCost = row.get("dailyCost") != null ? (BigDecimal) row.get("dailyCost") : BigDecimal.ZERO;
                    Long callCount = row.get("callCount") != null ? ((Number) row.get("callCount")).longValue() : 0L;
                    dataMap.put(date, new DailyCostTrendVO(date, dailyCost, callCount));
                }
            }
        }

        // 填充完整7天，缺失的补0
        List<DailyCostTrendVO> result = new ArrayList<>();
        for (LocalDate date : last7Days) {
            DailyCostTrendVO vo = dataMap.getOrDefault(date, new DailyCostTrendVO(date, BigDecimal.ZERO, 0L));
            result.add(vo);
        }
        return result;
    }
}