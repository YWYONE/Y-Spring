package com.example.yspringcore.boot;

import com.example.yspringcore.ioc.context.Container;
import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.web.utils.WebUtils;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import com.example.yspringcore.web.WebMvcConfiguration;

import java.util.Set;

/**
 * servlet container set up listener
 */
public class ContextLoaderInitializer implements ServletContainerInitializer {
    final Class<?> configClass;
    final PropertyResolver propertyResolver;

    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
        //pass servletcontext pointer to ioc
        WebMvcConfiguration.setServletContext(servletContext);
        //setup Ioc Container
        Container container=new Container(this.configClass,this.propertyResolver);
        //set up Filter and DispatcherServlet:
        //WebUtils.registerFilters(servletContext);
        WebUtils.registerDispatcherServlet(servletContext, this.propertyResolver);

    }
}
