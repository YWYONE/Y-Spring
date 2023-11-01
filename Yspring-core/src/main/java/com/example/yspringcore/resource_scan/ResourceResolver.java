package com.example.yspringcore.resource_scan;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

public class ResourceResolver {
    //pkg name
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }
    public <R> List<R> scan(Function<Resource,R> mapper){
        //pkg name -> pkg relative url
        String path=basePackage.replace(".","/");
        //pkg absolute url
        Enumeration<URL> urls=getClass().getClassLoader().getResources(path);
        while(urls.hasMoreElements()){
            URL url = urls.nextElement();
            String urlString = URLDecoder.decode(url.toString(), StandardCharsets.UTF_8);

        }


    }
}
