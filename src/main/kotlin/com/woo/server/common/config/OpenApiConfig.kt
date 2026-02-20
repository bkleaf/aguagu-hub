package com.woo.server.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("aguagu-hub API")
                .description("카드 결제 문자(SMS) 수신·파싱·저장·조회 API")
                .version("v1")
        )
}
