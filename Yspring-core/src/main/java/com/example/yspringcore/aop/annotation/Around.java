package com.example.yspringcore.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited // the annotation will be inherited by subclass
@Documented
public @interface Around {

    /**
     * Invocation handler bean name.
     */
    String value();

}
