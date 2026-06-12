package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sqldmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("指标管理系统 Open API")
                        .description("对外查询已启用指标的 SQL 模版与参数定义。若已配置 API 客户端，请在 Header 携带 X-API-Key。")
                        .version("v0.05"))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .schemaRequirement("ApiKeyAuth", new SecurityScheme()
                        .name("X-API-Key")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER));
    }
}
