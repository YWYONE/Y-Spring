package com.example.yspringcore.aop.resolver;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Create proxy by subclassing and override methods with interceptor.
 */
@Slf4j
public class ProxyResolver {
    ByteBuddy byteBuddy=new ByteBuddy();
    public  static  ProxyResolver instance;

    public static ProxyResolver getInstance() {
        if(instance==null){
            instance=new ProxyResolver();
        }
        return instance;
    }
    private ProxyResolver(){}
    public <T> T createProxy(T bean, InvocationHandler invocationHandler){
        Class <?> beanClass=bean.getClass();
        log.info("create proxy for bean {} @{}", beanClass.getName(), Integer.toHexString(bean.hashCode()));
        //generate proxy class
        Class<?> proxyClass=this.byteBuddy
                // create a subclass
                .subclass(beanClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                //intercept methods
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                return invocationHandler.invoke(bean,method,args);
                            }
                        }
                ))
                .make().load(beanClass.getClassLoader()).getLoaded();
        Object proxy;
        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
