package com.example.demo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class A {
    public A(){
        System.out.println("a creating");
        run();
    }
    @Autowired
    B b;
    void run(){
        System.out.println("a call b");
        b.run();

    }
}
