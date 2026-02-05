package com.ping.pingpicturebackend.controller;

import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康监检查接口
 */
@Slf4j
@RestController
@RequestMapping("/")
public class HealthController {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<?> health() {
        String currentTime = LocalDateTime.now().format(formatter);
        log.info("健康检查接口被调用 - 时间: {}", currentTime);

        // 可以添加更多健康检查逻辑
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", currentTime);
        healthInfo.put("service", "ping-picture-backend");

        return ResultUtils.success(healthInfo);
    }
}
