// DailyCostTrendVO.java
package com.ping.pingpicture.interfaces.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("每日成本趋势视图")
public class DailyCostTrendVO {
    @ApiModelProperty("日期")
    private LocalDate date;
    
    @ApiModelProperty("当日成本（元）")
    private BigDecimal dailyCost;
    
    @ApiModelProperty("当日调用次数")
    private Long callCount;
}