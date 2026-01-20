package com.ping.pingpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间排行分析请求体
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * TOP N
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
