package com.example.yspringcore.web;

import com.example.yspringcore.web.annotation.ResponseBody;
import com.example.yspringcore.web.exception.WebException;
import com.example.yspringcore.web.utils.JsonUtils;
import com.example.yspringcore.web.utils.PathUtils;
import com.example.yspringcore.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.annotation.Annotation;
@Slf4j
/**
 * url processer -> a controller's method
 */
public class Processor {
    static Result NOT_PROCESSED = new Result(false, null);
    //controller isResponseBody
    boolean isRest;
    //return result (json) -> response's body
    boolean isResponseBody;
    boolean isVoid;
    //crl regular match
    Pattern urlPattern;
    Object controller;
    //process method
    Method handlerMethod;
    Param[] methodParameters;
    public Processor(String httpMethod, boolean isRest, Object controller, Method method, String urlPattern) throws ServletException {
        this.isRest = isRest;
        this.isResponseBody = method.getAnnotation(ResponseBody.class) != null;
        this.isVoid = method.getReturnType() == void.class;
        this.urlPattern = PathUtils.compile(urlPattern);
        this.controller = controller;
        this.handlerMethod = method;
        Parameter[] params = method.getParameters();
        Annotation[][] paramsAnnos = method.getParameterAnnotations();
        this.methodParameters = new Param[params.length];
        for (int i = 0; i < params.length; i++) {
            this.methodParameters[i] = new Param(httpMethod, method, params[i], paramsAnnos[i]);
        }

        log.info("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
    }
    Result process(HttpServletRequest req, HttpServletResponse resp)throws  Exception{
        Matcher matcher=urlPattern.matcher(req.getRequestURI());
        if(matcher.matches()){
            Object [] finalParams=new Object[methodParameters.length];
            for(int i=0;i< finalParams.length;i++){
                Param param=methodParameters[i];
                finalParams[i] =switch (param.paramType){
                    case PATH_VARIABLE -> {
                        try{
                        String v=matcher.group(param.name);
                        yield convertToType(param.classType,v);}catch(IllegalArgumentException e){
                            throw new WebException("Path variable '" + param.name + "' not found.");
                        }
                    }
                    case REQUEST_PARAM -> {
                        String v=getOrDefault(req, param.name, param.defaultValue);
                        yield convertToType(param.classType,v);
                    }
                    case REQUEST_BODY -> {
                        BufferedReader reader = req.getReader();
                        yield JsonUtils.readJson(reader, param.classType);
                    }
                    case SERVLET_VARIABLE -> {
                        Class<?> classType = param.classType;
                        if (classType == HttpServletRequest.class) {
                            yield req;
                        } else if (classType == HttpServletResponse.class) {
                            yield resp;
                        } else if (classType == HttpSession.class) {
                            yield req.getSession();
                        } else if (classType == ServletContext.class) {
                            yield req.getServletContext();
                        } else {
                            throw new WebException("Could not determine argument type: " + classType);
                        }
                    }
                };

            }
            Object result=null;
            try {
                result = handlerMethod.invoke(controller, finalParams);
            }catch (Exception e){
                throw new WebException("controller process error");
            }
            return new Result(true,result);


        }
        return NOT_PROCESSED;

    }
    Object convertToType(Class<?> classType, String s) {
        if (classType == String.class) {
            return s;
        } else if (classType == boolean.class || classType == Boolean.class) {
            return Boolean.valueOf(s);
        } else if (classType == int.class || classType == Integer.class) {
            return Integer.valueOf(s);
        } else if (classType == long.class || classType == Long.class) {
            return Long.valueOf(s);
        } else if (classType == byte.class || classType == Byte.class) {
            return Byte.valueOf(s);
        } else if (classType == short.class || classType == Short.class) {
            return Short.valueOf(s);
        } else if (classType == float.class || classType == Float.class) {
            return Float.valueOf(s);
        } else if (classType == double.class || classType == Double.class) {
            return Double.valueOf(s);
        } else {
            throw new WebException("Could not determine argument type: " + classType);
        }
    }
    String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String s = request.getParameter(name);
        if (s == null) {
            if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                throw new WebException("Request parameter '" + name + "' not found.");
            }
            return defaultValue;
        }
        return s;
    }
}
