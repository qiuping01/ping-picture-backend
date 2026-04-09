package com.ping.pingpicture.infrastructure.mapper;

import com.ping.pingpicture.infrastructure.token.entity.PictureAuditTokenStats;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
* @author 21877
* @description 针对表【picture_audit_token_stats(图片 AI 调用 token 统计表)】的数据库操作Mapper
* @createDate 2026-04-09 15:12:10
* @Entity com.ping.pingpicture.infrastructure.token.entity.PictureAuditTokenStats
*/
@Mapper
public interface PictureAuditTokenStatsMapper extends BaseMapper<PictureAuditTokenStats> {

    /**
     * 统计指定时间范围内的总成本
     */
    @Select("SELECT SUM(cost) FROM picture_audit_token_stats WHERE create_time BETWEEN #{startTime} AND #{endTime}")
    BigDecimal sumCostByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按用户统计成本
     */
    @Select("SELECT user_id, SUM(cost) as total_cost, COUNT(*) as call_count " +
            "FROM picture_audit_token_stats " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY user_id " +
            "ORDER BY total_cost DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getUserCostStats(@Param("startTime") Date startTime,
                                               @Param("endTime") Date endTime,
                                               @Param("limit") int limit);

    /**
     * 按空间统计成本
     */
    @Select("SELECT space_id, SUM(cost) as total_cost, COUNT(*) as call_count " +
            "FROM picture_audit_token_stats " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} AND space_id IS NOT NULL " +
            "GROUP BY space_id " +
            "ORDER BY total_cost DESC")
    List<Map<String, Object>> getSpaceCostStats(@Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime);

}




