package com.jobportal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jobportal.audit.AuditLoggingFilter;

@Configuration
public class AuditConfiguration {

    @Autowired
    private AuditLoggingFilter auditLoggingFilter;

    @Bean
    public FilterRegistrationBean<AuditLoggingFilter> auditFilterRegistration() {
        FilterRegistrationBean<AuditLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(auditLoggingFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // Set order to ensure it runs early in the filter chain
        registrationBean.setName("auditLoggingFilter");
        return registrationBean;
    }
}
