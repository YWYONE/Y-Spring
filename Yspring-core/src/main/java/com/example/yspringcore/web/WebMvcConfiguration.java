package com.example.yspringcore.web;

import com.example.yspringcore.ioc.annotation.Autowired;
import com.example.yspringcore.ioc.annotation.Bean;
import com.example.yspringcore.ioc.annotation.Configuration;
import com.example.yspringcore.ioc.annotation.Value;
import jakarta.servlet.ServletContext;

import java.util.Objects;
@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    /**
     * Set by web listener.
     */
    public static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver( //
                               @Autowired ServletContext servletContext, //
                               @Value("${summer.web.freemarker.template-path:/WEB-INF/templates}") String templatePath, //
                               @Value("${summer.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(servletContext, templatePath, templateEncoding);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }
}
