package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Component;

@Component
public class B {
    void run(){
        System.out.println("B is running");
    }
}
