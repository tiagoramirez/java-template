package com.tiagoramirez.template.config;

import com.tiagoramirez.template.logging.adapters.in.web.HttpLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> loggingFilter(HttpLoggingFilter httpLoggingFilter) {
        FilterRegistrationBean<HttpLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(httpLoggingFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
