package com.example.yspringcore.ioc.context;

import jakarta.annotation.Nullable;
import lombok.Data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

@Data
public class BeanDef implements Comparable<BeanDef>{
    // global unique Bean Name:
    private final String name;
    // Bean's  Declaration type(can be different from instance's real type eg. @Configuration@Bean subclass)
    private final Class<?> beanClass;
    // Bean 's instance
    private Object instance = null;
    // or null:
    private final Constructor<?> constructor;
    // or null:
    private final String factoryName;
    // or null:
    private final Method factoryMethod;
    // Bean 's order:
    private final int order;
    // is @Primary ?
    private final boolean primary;

    private String initMethodName;
    private String destroyMethodName;

    private Method initMethod;
    private Method destroyMethod;
    public BeanDef(String name, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary, String initMethodName,
                   String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    public BeanDef(String name, Class<?> beanClass, String factoryName, Method factoryMethod, int order, boolean primary, String initMethodName,
                   String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }
    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    @Nullable
    public String getFactoryName() {
        return this.factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return this.factoryMethod;
    }
    @Nullable
    public Method getInitMethod() {
        return this.initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return this.destroyMethod;
    }

    @Nullable
    public String getInitMethodName() {
        return this.initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }
    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    public Object getRequiredInstance()  {
        if (this.instance == null) {
//            throw new BeanCreationException(String.format("Instance of bean with name '%s' and type '%s' is not instantiated during current stage.",
//                    this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "Bean instance is null.");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
//            throw new BeanCreationException(String.format("Instance '%s' of Bean '%s' is not the expected type: %s", instance, instance.getClass().getName(),
//                    this.beanClass.getName()));
        }
        this.instance = instance;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    @Override
    public String toString() {
        return "BeanDefinition [name=" + name + ", beanClass=" + beanClass.getName() + ", factory=" + getCreateDetail() + ", init-method="
                + (initMethod == null ? "null" : initMethod.getName()) + ", destroy-method=" + (destroyMethod == null ? "null" : destroyMethod.getName())
                + ", primary=" + primary + ", instance=" + instance + "]";
    }
    String getCreateDetail() {
        if (this.factoryMethod != null) {
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(t -> t.getSimpleName()).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "(" + params + ")";
        }
        return null;
    }

    @Override
    public int compareTo(BeanDef def) {
        int cmp = Integer.compare(this.order, def.order);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(def.name);
    }


}
