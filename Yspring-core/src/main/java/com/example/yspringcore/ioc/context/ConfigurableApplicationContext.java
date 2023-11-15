package com.example.yspringcore.ioc.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Used for BeanPostProcessor
 */
public interface ConfigurableApplicationContext  extends ApplicationContext {

    List<BeanDef> findBeanDefs(Class<?> type);

    @Nullable
    BeanDef findBeanDef(Class<?> type);

    @Nullable
    BeanDef findBeanDef(String name);

    @Nullable
    BeanDef findBeanDef(String name, Class<?> requiredType);

    Object createSingletonBean(BeanDef def);
}
