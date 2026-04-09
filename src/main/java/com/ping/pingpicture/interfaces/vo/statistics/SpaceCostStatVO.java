// SpaceCostStatVO.java
package com.ping.pingpicture.interfaces.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("空间成本统计视图")
public class SpaceCostStatVO {
    @ApiModelProperty("空间ID")
    private Long spaceId;
    @ApiModelProperty("总成本（元）")
    private BigDecimal totalCost;
    @ApiModelProperty("调用次数")
    private Long callCount;
}