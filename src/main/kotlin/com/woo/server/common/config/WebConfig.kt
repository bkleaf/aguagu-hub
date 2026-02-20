package com.woo.server.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * SPA(Vue Router history 모드) 지원을 위한 웹 설정.
 * - /m/  경로 → 모바일 프론트엔드 (classpath:/static-mobile/)
 * - /    경로 → PC 프론트엔드 (classpath:/static/)
 * /api, /swagger 등은 컨트롤러가 처리하므로 SPA 폴백 대상에서 제외된다.
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    /** /m 또는 /m/ 루트 접근 시 모바일 index.html로 포워딩 (빈 경로는 ResourceHandler가 처리하지 못함) */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/m").setViewName("forward:/m/index.html")
        registry.addViewController("/m/").setViewName("forward:/m/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 모바일 SPA (/m/**)  — 구체적 경로를 먼저 등록
        registry.addResourceHandler("/m/**")
            .addResourceLocations("classpath:/static-mobile/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requested = location.createRelative(resourcePath)
                    // 실제 파일이 존재하면 반환, 아니면 모바일 index.html로 SPA 폴백
                    return if (requested.exists() && requested.isReadable) {
                        requested
                    } else {
                        ClassPathResource("/static-mobile/index.html")
                    }
                }
            })

        // PC SPA (/**)
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requested = location.createRelative(resourcePath)
                    // 실제 파일이 존재하면 반환, 아니면 PC index.html로 SPA 폴백
                    return if (requested.exists() && requested.isReadable) {
                        requested
                    } else {
                        ClassPathResource("/static/index.html")
                    }
                }
            })
    }
}
