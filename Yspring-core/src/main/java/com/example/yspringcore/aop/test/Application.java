package com.example.yspringcore.aop.test;

import com.example.yspringcore.ioc.annotation.ComponentScan;
import com.example.yspringcore.ioc.context.Container;
import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.ioc.utils.ApplicationContextUtils;

import java.util.Properties;

@ComponentScan
public class Application {
    public static void main(String[] args) {
        Container container=new Container(Application.class,createPropertyResolver());
        A a=container.getBean("a");
        a.run();

    }
    static PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        var pr = new PropertyResolver(ps);
        return pr;
    }
}
