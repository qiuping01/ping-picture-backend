package com.ping.pingpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.ping.pingpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class PingPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PingPictureBackendApplication.class, args);
    }

}
