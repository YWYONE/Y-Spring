package com.example.yspringcore.web;

import com.example.yspringcore.ioc.utils.ClassUtils;
import com.example.yspringcore.web.annotation.PathVariable;
import com.example.yspringcore.web.annotation.RequestBody;
import com.example.yspringcore.web.annotation.RequestParam;
import jakarta.servlet.ServletException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import  java.lang.reflect.Method;

public class Param {
    //http_method:get/post
    String name;
    ParamType paramType;
    Class<?> classType;
    String defaultValue;
    public Param(String httpMethod, Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
        PathVariable pathVariable= ClassUtils.findAnnotation(annotations,PathVariable.class);
        RequestParam requestParam=ClassUtils.findAnnotation(annotations,RequestParam.class);
        RequestBody requestBody=ClassUtils.findAnnotation(annotations,RequestBody.class);
        int annoCount=(pathVariable==null? 0:1)+(requestBody==null ? 0:1)+(requestParam==null?0:1);
        if(annoCount>1){
            throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + method);
        }
        this.classType=parameter.getType();
        if(pathVariable!=null){
            name=pathVariable.value();
            paramType=ParamType.PATH_VARIABLE;
        }
        else if(requestParam!=null){
            name=requestParam.value();
            paramType=ParamType.REQUEST_PARAM;
            defaultValue= requestParam.defaultValue();
        }
        else if(requestBody!=null){
            paramType=ParamType.REQUEST_BODY;
        }else{
            paramType=ParamType.SERVLET_VARIABLE;
        }
    }
}
