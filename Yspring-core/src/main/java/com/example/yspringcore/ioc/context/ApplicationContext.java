package com.example.yspringcore.ioc.context;

import java.util.List;


public interface ApplicationContext extends AutoCloseable {


    boolean containsBean(String name);


    <T> T getBean(String name);


    <T> T findBean(String name, Class<T> requiredType);


    <T> T findBean(Class<T> requiredType);


    <T> List<T> findBeans(Class<T> requiredType);

    /**
     * close and invoke all bean's @destroy method
     */
    void close();

}
