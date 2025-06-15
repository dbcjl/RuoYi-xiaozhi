package com.rouyi.xiaozhi.web.core.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * 过滤器配置
 */
@Configuration
public class WebFilterConfig {

    /**
     * 添加ETag过滤器，主要是为了解决Content-Length的问题，设备固件的Http接口必须要设置Content-Length，否则会报错。
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new ShallowEtagHeaderFilter());
        filterRegistrationBean.addUrlPatterns("/api/ota/*");
        return filterRegistrationBean;
    }

}
