package com.example.yspringcore.ioc.utils;

import com.example.yspringcore.ioc.context.ApplicationContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * get container anywhere
 */
public class ApplicationContextUtils {
    private static ApplicationContext applicationContext=null;
    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(applicationContext,"application is null");
    }
    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextUtils.applicationContext = applicationContext;
    }
}
