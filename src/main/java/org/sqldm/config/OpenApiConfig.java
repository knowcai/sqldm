package org.sqldm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sqldmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("指标管理系统 Open API")
                        .description("对外查询已启用指标的 SQL 模版与参数定义。")
                        .version("v0.05"));
    }
}
