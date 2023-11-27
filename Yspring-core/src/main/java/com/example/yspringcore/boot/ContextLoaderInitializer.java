//package com.example.yspringcore.boot;
//
//import com.example.yspringcore.ioc.context.Container;
//import com.example.yspringcore.ioc.scan.PropertyResolver;
//import com.example.yspringcore.web.utils.WebUtils;
//import jakarta.servlet.ServletContainerInitializer;
//import jakarta.servlet.ServletContext;
//import jakarta.servlet.ServletException;
//
//import java.util.Set;
//
//public class ContextLoaderInitializer implements ServletContainerInitializer {
//    final Class<?> configClass;
//    final PropertyResolver propertyResolver;
//
//    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
//        this.configClass = configClass;
//        this.propertyResolver = propertyResolver;
//    }
//
//    @Override
//    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
//        //setup servletcontext
////        WebMvcConfiguration.setServletContext(ctx);
////        //setup Ioc Container
////        Container container=new Container(this.configClass,this.propertyResolver);
////        // set up Filter and DispatcherServlet:
////        WebUtils.registerFilters(ctx);
////        WebUtils.registerDispatcherServlet(ctx, this.propertyResolver);
//
//    }
//}
