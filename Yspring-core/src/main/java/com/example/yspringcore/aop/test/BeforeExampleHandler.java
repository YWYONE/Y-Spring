package com.example.yspringcore.aop.test;

import com.example.yspringcore.aop.handler.BeforeInvocationHandlerAdapter;
import com.example.yspringcore.ioc.annotation.Component;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
@Component
@Slf4j
public class BeforeExampleHandler extends BeforeInvocationHandlerAdapter {
    @Override
    public void before(Object proxy, Method method, Object[] args) {
        log.info("before {}()",method.getName());
    }
}
