package com.example.demo;

import org.springframework.stereotype.Component;

@Component
public class B {
    public B(){
        System.out.println("b creating");
    }
    void run(){
        System.out.println("B is running");
    }
}
