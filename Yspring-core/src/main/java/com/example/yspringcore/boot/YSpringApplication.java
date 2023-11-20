package com.example.yspringcore.boot;

import com.example.yspringcore.ioc.scan.PropertyResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Server;
import org.apache.catalina.startup.Tomcat;

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;

@Slf4j
public class YSpringApplication {
    public void start(Class<?> configClass) throws Exception{
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

        server.await();
    }
    public Server startTomcat(PropertyResolver propertyResolver){
        int port =propertyResolver.getProperty("${server.port:8080}",int.class);
        log.info("start tomcat at port{}",port);
        Tomcat tomcat=new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector().setThrowOnFailure(true);


    }

}
