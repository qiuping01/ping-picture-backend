// CostTrendRequest.java
package com.ping.pingpicture.interfaces.dto.statistics;

import lombok.Data;

import java.util.Date;

@Data
public class CostTrendRequest {
    /**
     * 开始时间
     */
    private Date startTime;
    
    /**
     * 结束时间
     */
    private Date endTime;
}