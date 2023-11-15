package com.example.yspringcore.ioc.utils;

import com.example.yspringcore.ioc.annotation.Bean;
import com.example.yspringcore.ioc.annotation.Component;
import com.example.yspringcore.ioc.exception.IocException;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassUtils {
    /**
     * recursion  find Annotation eg.@Configuration use @Component
     *
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A a = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            //if belongs to Y-Spring annotation then recursion find
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        //Duplicate
                    }
                    a = found;
                }
            }
        }
        return a;
    }
    /**
     *  find Annotation in Anno[]
     *
     */
    @Nullable
    public static <A extends Annotation> A findAnnotation(Annotation[] annos, Class<A> annoClass) {
        for(Annotation a:annos){
            if(annoClass.isInstance(a) ){
                return (A) a;
            }
        }
        return null;
    }
    /**
     * Get bean name (@Component's value or class's name)
     *
     * <code>
     * @Component
     * public class Hello {}
     * </code>
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 查找@Component:
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            // @Component exist:
            name = component.value();
        } else {
            // not found @Component，recursion  find @Component:
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (name.isEmpty()) {
            // default name: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * Get non-arg method by @PostConstruct or @PreDestroy. Not search in super class
     * @param clazz
     * @param annoClass
     * @return
     */
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass){
        List<Method> list= Arrays.stream(clazz.getDeclaredMethods()).filter(a->a.isAnnotationPresent(annoClass)).map(a->{
            if(a.getParameterCount()!=0){
                throw new IocException(String.format("Method '%s' with @%s must not have argument:%s",a.getName(),annoClass.getSimpleName()));
            }
            return a;
        }).collect(Collectors.toList());
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        throw new IocException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));

    }


    /**
     * Get bean name by:
     *
     * <code>
     * @Bean
     * Hello createHello() {}
     * </code>
     */
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

}
