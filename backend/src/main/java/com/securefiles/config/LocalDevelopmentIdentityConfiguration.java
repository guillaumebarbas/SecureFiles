package com.securefiles.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Configuration
@Profile("local")
@EnableConfigurationProperties(LocalDevelopmentIdentityProperties.class)
public class LocalDevelopmentIdentityConfiguration {

    @Bean
    public FilterRegistrationBean<LocalDevelopmentPrincipalFilter> localDevelopmentPrincipalFilter(
            LocalDevelopmentIdentityProperties properties) {
        FilterRegistrationBean<LocalDevelopmentPrincipalFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LocalDevelopmentPrincipalFilter(properties.ownerId()));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}