package com.example.yspringcore.boot.test;

import com.example.yspringcore.web.annotation.Controller;
import com.example.yspringcore.web.annotation.GetMapping;

@Controller
public class Contr {
    @GetMapping("/")
    public void funa(){
        System.out.println("test !!!!");
    }

}
