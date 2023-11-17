package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.annotation.Autowired;
import com.example.yspringcore.ioc.annotation.Component;

@Component
public class BB {
    public BB(@Autowired AA aa){}

}
