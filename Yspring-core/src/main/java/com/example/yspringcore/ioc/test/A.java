package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Autowired;
import com.example.yspringcore.ioc.annotation.Component;

@Component
public class A {
    @Autowired
    B b;
    void run(){
        System.out.println("a call b");
        b.run();

    }
}
