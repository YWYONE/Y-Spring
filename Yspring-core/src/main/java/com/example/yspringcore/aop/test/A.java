package com.example.yspringcore.aop.test;

import com.example.yspringcore.aop.annotation.Around;
import com.example.yspringcore.ioc.annotation.Component;

@Around("beforeExampleHandler")
@Component
public class A {
    public void run(){
        System.out.println(" a run");
    }
}
