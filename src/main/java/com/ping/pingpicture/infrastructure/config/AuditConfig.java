package com.ping.pingpicture.infrastructure.config;

import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 使用
 */
@Configuration
@Slf4j
public class AuditConfig {

    @Bean
    public Retryer<Object> auditRetryer() {
        // 1. 定义重试条件：当发生 Exception 异常时重试
        Retryer<Object> retryer = RetryerBuilder.newBuilder()
                .retryIfException() // 发生任何异常都重试
                // .retryIfRuntimeException() // 发生 RuntimeException 重试
                // .retryIfResult(result -> result == null) // 返回结果为空重试

                // 2. 定义等待策略：指数退避，初始延迟1秒，乘以2，最多不超过60秒
                .withWaitStrategy(WaitStrategies.exponentialWait(1, 60, TimeUnit.SECONDS))

                // 3. 定义停止策略：最大重试次数为3次
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))

                // 4. 定义超时限制：单次任务执行超过30秒则抛出异常并触发重试
//                .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(30, TimeUnit.SECONDS))

                // 5. 添加重试监听器：用于记录每次重试的详细信息
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasException()) {
                            log.warn("AI 审核第 {} 次调用失败，异常: {}",
                                    attempt.getAttemptNumber(),
                                    attempt.getExceptionCause().getMessage());
                        } else {
                            log.info("AI 审核第 {} 次调用成功", attempt.getAttemptNumber());
                        }
                    }
                })
                .build();
        return retryer;
    }
}