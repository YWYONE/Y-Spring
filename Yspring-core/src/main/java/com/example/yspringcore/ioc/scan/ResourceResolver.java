package com.example.yspringcore.ioc.scan;

import com.example.yspringcore.ioc.scan.Resource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * scan classes  in pkg path
 */
@Slf4j
public class ResourceResolver {
    //pkg name
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     *
     * @param mapper  caller define convert ops
     * @return
     * @param <R>
     */
    public <R> List<R> scan(Function<Resource,R> mapper){
        //pkg name -> pkg relative url
        List<R> rs = new ArrayList<>();
        String relativePath=basePackage.replace(".","/");
        try {
            //pkg absolute url
            Enumeration<URL> urls = getContextClassLoader().getResources(relativePath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                URI uri = url.toURI();
                String urlString = URLDecoder.decode(url.toString(), StandardCharsets.UTF_8);
                String baseUrlString = urlString.substring(0, urlString.length() - relativePath.length());
                if (baseUrlString.startsWith("file:")) {
                    baseUrlString=baseUrlString.substring(5);
                }
                if (urlString.startsWith("jar")) {
                    scanFile(true, baseUrlString, jarUriToPath(relativePath, url), rs, mapper);
                } else {
                    scanFile(false, baseUrlString, Paths.get(url.toURI()), rs, mapper);
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return rs;


    }
    Path jarUriToPath(String relativePath, URL jarUrl) throws IOException, URISyntaxException {
        return FileSystems.newFileSystem(jarUrl.toURI(), Map.of()).getPath(relativePath);
    }
    public <R> void scanFile(boolean isJar, String base, Path root, List<R> rs, Function<Resource, R> mapper) throws IOException {
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(base, file.toString());
            } else {
                String path = file.toString();
                String name = path.substring(base.length());
                res = new Resource("file:" + path, name);
            }
            log.info("find resource: {}",res);
            //convert
            R r = mapper.apply(res);
            if (r != null) {
                rs.add(r);
            }
        });
    }

    /**
     * first get servlet's classLoader : search  /WEB-INF/classes  /WEB-INF/lib (jar)
     * second get current class's classLoader
     * @return
     */
    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }
}
