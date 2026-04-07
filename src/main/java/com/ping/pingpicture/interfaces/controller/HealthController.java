package com.ping.pingpicture.interfaces.controller;

import cn.hutool.json.JSONUtil;
import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 健康监检查接口
 */
@Slf4j
@RestController
@RequestMapping("/")
public class HealthController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

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

    /**
     * 队列测试
     */
    @GetMapping("/queue/add")
    public BaseResponse<?> queueAdd(String name) {
        CompletableFuture.runAsync(() -> {
            log.info("任务执行中：{}。执行人：{}", name, Thread.currentThread().getName());
            try {
                Thread.sleep(180000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, threadPoolExecutor);

        return ResultUtils.success("任务已添加到队列");
    }

    /**
     * 队列测试
     */
    @GetMapping("/queue/get")
    public BaseResponse<String> queueGet() {
        Map<String, Object> map = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度", size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数", taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数", completedTaskCount);
        long activeCount = threadPoolExecutor.getActiveCount();
        map.put("正常工作的线程数", activeCount);
        return ResultUtils.success(JSONUtil.toJsonStr(map));


    }

}
