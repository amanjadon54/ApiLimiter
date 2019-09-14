package com.assignment.blueoptima.configuration;

import com.assignment.blueoptima.interceptor.FilteringInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiLimitConfiguration implements WebMvcConfigurer {

    @Autowired
    private FilteringInterceptor filteringInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(filteringInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/admin/**").excludePathPatterns("/error/**");
    }
}
