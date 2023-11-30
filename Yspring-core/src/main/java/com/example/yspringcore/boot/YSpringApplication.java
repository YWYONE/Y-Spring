package com.example.yspringcore.boot;

import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.web.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Set;

@Slf4j
public class YSpringApplication {
    public void start(String webDir, String baseDir, Class<?> configClass) throws Exception{
        final long startTime = System.currentTimeMillis();
        final int javaVersion = Runtime.version().feature();
        final long pid = ManagementFactory.getRuntimeMXBean().getPid();
        final String user = System.getProperty("user.name");
        final String pwd = Paths.get("").toAbsolutePath().toString();
        log.info("Starting {} using Java {} with PID {} (started by {} in {})", configClass.getSimpleName(), javaVersion, pid, user, pwd);
        //read application.yml config
        var propertyResolver = WebUtils.createPropertyResolver();
        //create tomcat server
        var server = startTomcat(webDir, baseDir, configClass, propertyResolver);
        final long endTime = System.currentTimeMillis();
        final String appTime = String.format("%.3f", (endTime - startTime) / 1000.0);
        final String jvmTime = String.format("%.3f", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
        log.info("Started {} in {} seconds (process running for {})", configClass.getSimpleName(), appTime, jvmTime);
        server.await();
    }
    public Server startTomcat(String webDir, String baseDir,Class<?> configClass, PropertyResolver propertyResolver) throws  Exception{
        int port =propertyResolver.getProperty("${server.port:8080}",int.class);
        log.info("start tomcat at port{}",port);
        Tomcat tomcat=new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector().setThrowOnFailure(true);
        //add a default webapp ,mount on '/'
        Context ctx = tomcat.addWebapp("", new File(webDir).getAbsolutePath());
        // setup application directory
        WebResourceRoot webResourceRoot=new StandardRoot(ctx);
        webResourceRoot.addPreResources(new DirResourceSet(webResourceRoot,"/WEB-INF/classes",new File(baseDir).getAbsolutePath(),"/"));
        ctx.setResources(webResourceRoot);
        // setup ServletContainerInitializer listener
        ctx.addServletContainerInitializer(new ContextLoaderInitializer(configClass, propertyResolver), Set.of());
        tomcat.start();
        log.info("Tomcat started at port {}...", port);
        return  tomcat.getServer();
    }

}
