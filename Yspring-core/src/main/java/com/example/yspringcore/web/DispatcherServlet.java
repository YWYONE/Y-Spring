package com.example.yspringcore.web;

import com.example.yspringcore.ioc.context.ApplicationContext;
import com.example.yspringcore.ioc.context.Container;
import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.web.annotation.Controller;
import com.example.yspringcore.web.annotation.GetMapping;
import com.example.yspringcore.web.annotation.PostMapping;
import com.example.yspringcore.web.annotation.RestController;
import com.example.yspringcore.web.exception.WebException;
import com.example.yspringcore.web.utils.JsonUtils;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.http.WebSocketHandshakeException;
import java.util.ArrayList;
import java.util.List;
@Slf4j
public class DispatcherServlet extends HttpServlet {
    ApplicationContext applicationContext;
    ViewResolver viewResolver;
    //static resource path
    String resourcePath;
    //ico resource path
    String faviconPath;

    List<Processor> getProcessors = new ArrayList<>();
    List<Processor> postProcessors = new ArrayList<>();
    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver properyResolver) {
        this.applicationContext = applicationContext;
        this.viewResolver = applicationContext.findBean(ViewResolver.class);
        this.resourcePath = properyResolver.getProperty("${y-spring.web.static-path:/static/}");
        this.faviconPath = properyResolver.getProperty("${y-spring.web.favicon-path:/favicon.ico}");
        if (!this.resourcePath.endsWith("/")) {
            this.resourcePath = this.resourcePath + "/";
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("init {}",this.getClass().getName());
        // scan @Controller and @RestController:

        for(var def:((Container)this.applicationContext).
                findBeanDefs(Object.class) ){

            Class <?>beanClass=def.getBeanClass();
            Object bean=def.getInstance();
            Controller controller=beanClass.getAnnotation(Controller.class);
            RestController restController=beanClass.getAnnotation(RestController.class);
            if (controller != null && restController != null) {
                throw new ServletException("Found @Controller and @RestController both on class: " + beanClass.getName());
            }
            if(controller!=null){
                addController(false, def.getName(), bean);
            }else if(restController!=null){
                addController(true, def.getName(), bean);
            }

        }
    }
    @Override
    public void destroy() {
        this.applicationContext.close();
    }
    void addController(boolean isRest, String name, Object instance) throws ServletException {
        log.info("add {} controller '{}': {}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        addMethods(isRest, name, instance, instance.getClass());
    }
    void addMethods(boolean isRest, String name, Object instance, Class<?> type) throws ServletException {
        for(Method m:type.getDeclaredMethods()){
            GetMapping getMapping=m.getAnnotation(GetMapping.class);
            PostMapping postMapping=m.getAnnotation(PostMapping.class);
            if(getMapping!=null){
                checkMethod(m);
                this.getProcessors.add(new Processor("get",isRest,instance,m,getMapping.value()));
            }
            if(postMapping!=null){
                checkMethod(m);
                this.getProcessors.add(new Processor("post",isRest,instance,m,postMapping.value()));
            }
        }
        //inherient super class method even though no @Controller
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addMethods(isRest, name, instance, superClass);
        }
    }
    void checkMethod(Method m) throws ServletException {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new ServletException("Cannot do URL mapping to static method: " + m);
        }
        m.setAccessible(true);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url =req.getRequestURI();
        if(url.equals(this.faviconPath)||url.equals(this.resourcePath)){
            doResource(url,req,resp);
        }else{
            doProcess(req,resp,this.getProcessors);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req,resp,this.postProcessors);
    }
    void doResource(String url ,HttpServletRequest req,HttpServletResponse resp) throws IOException{
        ServletContext servletContext=req.getServletContext();
        try(InputStream inputStream=servletContext.getResourceAsStream(url)){
            if(inputStream==null){
                resp.sendError(404,"resource not found");
            }else{
                // guess content type:
                String file = url;
                int n = url.lastIndexOf('/');
                if (n >= 0) {
                    file = url.substring(n + 1);
                }
                String mime = servletContext.getMimeType(file);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                inputStream.transferTo(output);
                output.flush();
            }
        }
    }
    void doProcess(HttpServletRequest req,HttpServletResponse resp,List<Processor> processors) throws ServletException, IOException{
        try {
            for (Processor processor : processors) {
                Result result = processor.process(req, resp);
                //match
                if (result.processed()) {
                    Object rs = result.returnObject();
                    if (processor.isRest) {
                        // send rest response:
                        if (!resp.isCommitted()) {
                            resp.setContentType("application/json");
                        }
                        if (processor.isResponseBody) {
                            if (rs instanceof String s) {
                                PrintWriter pw = resp.getWriter();
                                pw.write(s);
                                pw.flush();
                            } else if (rs instanceof byte[] data) {
                                ServletOutputStream output = resp.getOutputStream();
                                output.write(data);
                                output.flush();
                            } else {
                                // error:
                                throw new ServletException("Unable to process REST result when handle url: " + req.getRequestURI());
                            }
                        } else if (!processor.isVoid) {
                            PrintWriter pw = resp.getWriter();
                            JsonUtils.writeJson(pw, rs);
                            pw.flush();
                        }

                    } else {
                        //mvc rendering
                        if (!resp.isCommitted()) {
                            resp.setContentType("text/html");
                        }
                        if (rs instanceof String s) {
                            if (processor.isResponseBody) {
                                // send as response body:
                                PrintWriter pw = resp.getWriter();
                                pw.write(s);
                                pw.flush();
                            } else if (s.startsWith("redirect:")) {
                                // send redirect:
                                resp.sendRedirect(s.substring(9));
                            } else {
                                // error:
                                throw new ServletException("Unable to process String result when handle url: " + req.getRequestURI());
                            }
                        } else if (rs instanceof byte[] data) {
                            if (processor.isResponseBody) {
                                // send as response body:
                                ServletOutputStream output = resp.getOutputStream();
                                output.write(data);
                                output.flush();
                            } else {
                                // error:
                                throw new ServletException("Unable to process byte[] result when handle url: " + req.getRequestURI());
                            }
                        } else if (rs instanceof ModelAndView mv) {
                            String view = mv.getViewName();
                            if (view.startsWith("redirect:")) {
                                // send redirect:
                                resp.sendRedirect(view.substring(9));
                            } else {
                                this.viewResolver.render(view, mv.getModel(), req, resp);
                            }
                        } else if (!processor.isVoid && rs != null) {
                            // error:
                            throw new ServletException("Unable to process " + rs.getClass().getName() + " result when handle url: " + req.getRequestURI());
                        }
                    }
                    return;
                }
            }
            // not found:
            resp.sendError(404, "Not Found");
        }catch (RuntimeException | ServletException | IOException e){
            throw e;
        }catch (Exception e){
            throw  new WebException();
        }

    }

}
