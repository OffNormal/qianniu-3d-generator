package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("七牛云3D模型生成应用 API文档")
                        .description("基于AI的智能3D模型生成平台，提供文本生成3D模型、图片生成3D模型等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .url("https://github.com/your-org/qiniu-3d-generator")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}