package com.ping.pingpicture;

import com.ping.pingpicture.infrastructure.bizmq.MqInitMain;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.ping.pingpicture.infrastructure.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class PingPictureBackendApplication {

    public static void main(String[] args) {
        // 初始化消息队列
//        MqInitMain.doInit();
        SpringApplication.run(PingPictureBackendApplication.class, args);
    }
}
