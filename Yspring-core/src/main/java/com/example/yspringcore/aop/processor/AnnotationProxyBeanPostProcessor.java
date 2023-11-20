package com.example.yspringcore.aop.processor;


import com.example.yspringcore.aop.exception.AopException;
import com.example.yspringcore.aop.resolver.ProxyResolver;
import com.example.yspringcore.ioc.context.BeanDef;
import com.example.yspringcore.ioc.context.BeanPostProcessor;
import com.example.yspringcore.ioc.context.Container;
import com.example.yspringcore.ioc.utils.ApplicationContextUtils;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract  class AnnotationProxyBeanPostProcessor<T extends Annotation> implements BeanPostProcessor {
    Map<String, Object> originBeans=new HashMap<>();
    Class<T> annoClass;
    public AnnotationProxyBeanPostProcessor(){
        annoClass=getParameterizedType();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass=bean.getClass();
        T anno=beanClass.getAnnotation(annoClass);
        if(anno!=null){
            String handlerName;
            try {
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopException(String.format("@%s must have value() returned String type.", this.annoClass.getSimpleName()), e);

            }
            Object proxy=createProxy(beanClass,bean,handlerName);
            originBeans.put(beanName,bean);
            return proxy;
        }else{
            return bean;
        }
    }

    private Object createProxy(Class<?> beanClass,Object bean,String handlerName){
        Container container=(Container) ApplicationContextUtils.getRequiredApplicationContext();
        BeanDef beanDef=container.findBeanDef(handlerName);
        if (beanDef == null) {
            throw new AopException(String.format("@%s proxy handler '%s' not found.", this.annoClass.getSimpleName(), handlerName));
        }
        Object handlerBean=beanDef.getInstance();
        if(handlerBean==null){
            handlerBean=container.createSingletonBean(beanDef);
        }
        if(handlerBean instanceof InvocationHandler handler){
            return ProxyResolver.getInstance().createProxy(bean,handler);
        }else {
            throw new AopException(String.format("@%s proxy handler '%s' is not type of %s.", this.annoClass.getSimpleName(), handlerName,
                    InvocationHandler.class.getName()));
        }

    }

    /**
     * get T
     * @return
     */
    private Class<T> getParameterizedType(){
        // get superclass with Generics
        Type type=getClass().getGenericSuperclass();
        if(!(type instanceof ParameterizedType)){
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType parameterizedType=(ParameterizedType) type;
        Type [] types=parameterizedType.getActualTypeArguments();
        if(types.length!=1){
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type t=types[0];//T
        if(!(t instanceof Class<?>)){
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<T>) t;
    }

}
