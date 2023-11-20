package com.example.yspringcore.aop.test;

import com.example.yspringcore.aop.processor.AroundProxyBeanPostProcessor;
import com.example.yspringcore.ioc.annotation.Bean;
import com.example.yspringcore.ioc.annotation.Configuration;

@Configuration
public class AopConfigration {
    @Bean
    AroundProxyBeanPostProcessor aroundProxyBeanPostProcessor(){
        return new AroundProxyBeanPostProcessor();
    }

}
