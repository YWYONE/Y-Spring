package com.example.yspringcore.web.utils;

import jakarta.servlet.ServletException;

import java.util.regex.Pattern;

public class PathUtils {
    public static Pattern compile(String path) throws ServletException {
        //   /{param} -> /(?<param>[^/]*)  named capture group
        //   eg. /value   s=matcher.group("param");  -> s="value"
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z0-9]*)\\}", "(?<$1>[^/]*)");
        if (regPath.indexOf('{') >= 0 || regPath.indexOf('}') >= 0) {
            throw new ServletException("Invalid path: " + path);
        }
        System.out.println(regPath);
        return Pattern.compile("^" + regPath + "$");
    }

    public static void main(String[] args) throws  Exception{
        compile("/user/{id}");
    }

}
