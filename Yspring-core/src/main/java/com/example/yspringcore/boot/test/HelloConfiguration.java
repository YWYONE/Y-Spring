package com.example.yspringcore.boot.test;

import com.example.yspringcore.boot.YSpringApplication;
import com.example.yspringcore.ioc.annotation.ComponentScan;
import com.example.yspringcore.ioc.annotation.Configuration;
import com.example.yspringcore.ioc.annotation.Import;
import com.example.yspringcore.web.WebMvcConfiguration;

@ComponentScan
@Configuration
@Import({ WebMvcConfiguration.class })
public class HelloConfiguration {
    public static void main(String[] args) throws Exception{
        YSpringApplication.run("src/main/webapp","target/classes", HelloConfiguration.class,args);
    }
}
