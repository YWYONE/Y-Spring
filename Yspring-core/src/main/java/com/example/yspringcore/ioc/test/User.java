package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Autowired;
import com.example.yspringcore.ioc.annotation.Component;

@Component
public class User {
    public User(@Autowired A a){
        System.out.println("User is creating");
        a.run();
    }
}
