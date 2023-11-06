package com.example.yspringcore.ioc.test;

import com.example.yspringcore.ioc.scan.ResourceResolver;

import java.util.List;

public class ResourceScanTest {
    public static void main(String[] args) {
        ResourceResolver rr=new ResourceResolver("com.example");
        List<String> list=rr.scan(x->{
            String name=x.name();
            //filter class
            if(name.endsWith(".class")){
                // adapt windows '\'  linux and mac '/'  -> '.'
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        list.stream().forEach(a->{
            System.out.println(a);
        });

    }
}
