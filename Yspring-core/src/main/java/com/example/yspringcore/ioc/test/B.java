package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Autowired;
import com.example.yspringcore.ioc.annotation.Component;

@Component
public class B {
    @Autowired
    A a;
    void run(){
        System.out.println("B is running");
    }
}
