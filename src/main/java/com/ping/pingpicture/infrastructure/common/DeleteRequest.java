package com.ping.pingpicture.infrastructure.common;

import lombok.Data;

/**
 * 通用的删除请求类
 */
@Data
public class DeleteRequest {

    // 包含 id 请求类即可
    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
