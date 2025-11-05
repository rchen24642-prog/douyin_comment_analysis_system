package com.example.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // ✅ 允许所有域名
        config.addAllowedHeader("*");        // ✅ 允许所有请求头
        config.addAllowedMethod("*");        // ✅ 允许所有请求方式（GET、POST、PUT、DELETE）
        config.setAllowCredentials(true);    // ✅ 允许携带 Cookie
        config.setMaxAge(3600L);             // ✅ 预检请求缓存时间（秒）

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
