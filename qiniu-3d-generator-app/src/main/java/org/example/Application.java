package org.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 3D模型生成应用启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("org.example.infrastructure.persistent.dao")
@ComponentScan(basePackages = {
    "org.example.trigger",
    "org.example.infrastructure",
    "org.example.domain",
    "org.example.service",
    "org.example.config"
})
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("3D模型生成应用启动成功！");
        System.out.println("API文档地址: http://localhost:8091/swagger-ui/index.html");
    }
}

