package com.example.demo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class User {
    public User(@Autowired A a){
        System.out.println("User is creating");
        //a.run();
    }
    public static void run(){
        System.out.println("dsfdsfsdfdsfds");
    }

}
