package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.ComponentScan;
import com.example.yspringcore.ioc.context.Container;
import com.example.yspringcore.ioc.scan.PropertyResolver;

import java.util.Properties;

@ComponentScan
public class Application {
    public static void main(String[] args) {
        Container container=new Container(Application.class,createPropertyResolver());
        container.findBean(A.class).run();

    }
    static PropertyResolver  createPropertyResolver() {
        var ps = new Properties();
        var pr = new PropertyResolver(ps);
        return pr;
    }
}
