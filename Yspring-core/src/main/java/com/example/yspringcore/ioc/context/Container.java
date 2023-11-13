package com.example.yspringcore.ioc.context;

import com.example.yspringcore.ioc.annotation.*;
import com.example.yspringcore.ioc.exception.IocException;
import com.example.yspringcore.ioc.scan.ResourceResolver;
import com.example.yspringcore.ioc.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Container {
    Map<String, BeanDef> beans;
    public Container(Class<?> configClass){
        Set<String> beanClassName=scanClassName(configClass);
        beans=createBeans(beanClassName);
    }

    public Set<String> scanClassName(Class<?> configClass){
        Set<String> classNameSet=new HashSet<>();
        ComponentScan componentScan= ClassUtils.findAnnotation(configClass, ComponentScan.class);
        //pkg names
        String[] scanPackages;
        if(componentScan==null||componentScan.value()==null||componentScan.value().length==0){
            scanPackages=new String[] { configClass.getPackage().getName() };
        }else{
            scanPackages=componentScan.value();
        }
        log.info("component scan in pkg :{}", Arrays.toString(scanPackages));
        for(String s:scanPackages){
            log.info("scan pkg {}",s);
            ResourceResolver resourceResolver=new ResourceResolver(s);
            List<String> classList=resourceResolver.scan(x->{
                String name=x.name();
                //filter class
                if(name.endsWith(".class")){
                    // adapt windows '\'  linux and mac '/'  -> '.'
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            classNameSet.addAll(classList);

        }
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                if (classNameSet.contains(importClassName)) {
                    log.info("ignore import: {}" + importClassName + " for it is already been scanned.");
                } else {
                    log.info("class found by import: {}", importClassName);
                    classNameSet.add(importClassName);
                }
            }
        }
        return classNameSet;

    }
    public Map<String, BeanDef> createBeans(Set<String> classNameSet){
        Map<String, BeanDef> beans=new HashMap<>();
        for(String name:classNameSet){
            Class<?> clazz=null;
            try {
                clazz=Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }
            Component component=ClassUtils.findAnnotation(clazz,Component.class);
            //has @Component
            if(component!=null){
                log.info("found component :{}",clazz.getName());
                String beanName=ClassUtils.getBeanName(clazz);
                BeanDef beanDef =new BeanDef(
                        beanName,clazz,getSuitableConstructor(clazz),getOrder(clazz),clazz.isAnnotationPresent(Primary.class),
                        // named init / destroy method:
                        null, null,
                        // init method:
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // destroy method:
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class)   );
                addBeanDefinitions(beans, beanDef);
                log.info("define bean {}", beanDef);
                Configuration configuration=ClassUtils.findAnnotation(clazz,Configuration.class);
                if(configuration!=null){
                    scanFactoryMethods(name,clazz,beans);
                }

            }


        }
        return beans;

    }
    /**
     * Get public constructor or non-public constructor as fallback.
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new IocException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new IocException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }
    /**
     * Get order
     *
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }
    /**
     * Check and add bean
     */
    void addBeanDefinitions(Map<String, BeanDef> defs, BeanDef def) {
        if (defs.put(def.getName(), def) != null) {
            throw new IocException("Duplicate bean name: " + def.getName());
        }
    }
    /**
     * Scan factory method that annotated with @Bean
     *
     * <code>
     * @Configuration
     * public class Hello {
     *     @Bean
     *     ZoneId createZone() {
     *         return ZoneId.of("Z");
     *     }
     * }
     * </code>
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDef> defs) {
        for(Method method:clazz.getDeclaredMethods()){
            Bean bean=method.getAnnotation(Bean.class);
            if(bean!=null){
                int mod=method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new IocException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new IocException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new IocException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new IocException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new IocException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                BeanDef def = new BeanDef(ClassUtils.getBeanName(method), beanClass, factoryBeanName, method, getOrder(method),
                        method.isAnnotationPresent(Primary.class),
                        // init method:
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        // destroy method:
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        // @PostConstruct / @PreDestroy method:
                        null, null);
                addBeanDefinitions(defs,def);
                log.info("define bean {}", def);



            }

        }
    }
    /**
     * Get order by:
     *
     * <code>
     * @Order(100)
     * @Bean
     * Hello createHello() {
     *     return new Hello();
     * }
     * </code>
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * get BeanDef by name
     * @param name
     * @return
     */
    @Nullable
    public BeanDef findBeanDef(String name){
        return beans.get(name);
    }
    /**
     * get BeanDef by type
     * @param clazz
     * @return
     */
    @Nullable
    public BeanDef findBeanDef(Class<?> clazz){
        List<BeanDef> defs=findBeanDefs(clazz);
        if(defs.isEmpty()){
            return null;
        }else if (defs.size()==1){
            return defs.get(0);
        }
        List<BeanDef> primaryDefs=defs.stream().filter(def-> def.isPrimary()).collect(Collectors.toList());
        if(primaryDefs.isEmpty()){
            throw new IocException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", clazz.getName()));
        }else if(primaryDefs.size()==1){
            return primaryDefs.get(0);
        }else{
            throw new IocException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", clazz.getName()));
        }
    }
    /**
     * get BeanDef by name
     * @param clazz
     * @return
     */
    public List<BeanDef> findBeanDefs(Class<?> clazz){
        List<BeanDef> rs=beans.values().stream().filter(def->
            clazz.isAssignableFrom(def.getClass())
        ).collect(Collectors.toList());
        return rs;
    }

}
