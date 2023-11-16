package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Bean;
import com.example.yspringcore.ioc.annotation.Configuration;

@Configuration
public class ConfigurationC {
    @Bean
    D getD1(){
       return new D(1);
    }
    @Bean
    D getD2(){
        return new D(2);
    }
}
