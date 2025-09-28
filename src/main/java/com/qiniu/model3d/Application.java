package com.qiniu.model3d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 3D模型生成器应用主类
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EntityScan("com.qiniu.model3d.entity")
@EnableJpaRepositories("com.qiniu.model3d.repository")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}