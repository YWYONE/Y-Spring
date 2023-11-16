package com.example.yspringcore.ioc.context;

import com.example.yspringcore.ioc.annotation.*;
import com.example.yspringcore.ioc.exception.IocException;
import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.ioc.scan.ResourceResolver;
import com.example.yspringcore.ioc.utils.ApplicationContextUtils;
import com.example.yspringcore.ioc.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Container implements ConfigurableApplicationContext{
    Map<String, BeanDef> beans;
    PropertyResolver propertyResolver;
    //detect strong circular dependency
    Set<String> creatingBeanNames;
    List<BeanPostProcessor> beanPostProcessors=new ArrayList<>();
    public Container(Class<?> configClass, PropertyResolver propertyResolver){
        ApplicationContextUtils.setApplicationContext(this);
        this.propertyResolver=propertyResolver;
        Set<String> beanClassName=scanClassName(configClass);
        beans=createBeanDefs(beanClassName);
        this.creatingBeanNames=new HashSet<>();
        createConfigurationFactoryBeans();
        List<BeanPostProcessor> processors=this.beans.values().stream()
                        .filter(def->this.isBeanPostProcessorDef(def))
                                .map(def->{
                                    return (BeanPostProcessor)createSingletonBean(def);
                                }).collect(Collectors.toList());
        beanPostProcessors=processors;
        createNormalBeans();
        this.beans.values().forEach(def->{
            injectBeans(def,def.getBeanClass());
        });
        this.beans.values().forEach(def->{
            invokeMethod(def.getInstance(),def.getInitMethod(),def.getInitMethodName());
        });
    }

    /**
     * inject dependency
     * scan clazz's field&setter  inject bean instance(def's instance)
     * @param def
     * @param clazz
     */
    void injectBeans(BeanDef def,Class<?> clazz){
        Object targetInstance=getProxiedInstance(def);
        try{
            for(Field field:clazz.getDeclaredFields()){
                injectBean(def,targetInstance,clazz,field);
            }
            for(Method method:clazz.getDeclaredMethods()){
                injectBean(def,targetInstance,clazz,method);
            }
            //superClass's dependency inject sonClass instance
            Class<?> superClazz=clazz.getSuperclass();
            if(superClazz!=null){
                injectBeans(def,superClazz);
            }
        }catch (ReflectiveOperationException e){
            throw  new IocException(e);
        }

    }

    /**
     * get def's instance
     * if be proxy then return origin instance
     * @param def
     * @return
     */
    Object getProxiedInstance(BeanDef def){
        Object instance =def.getInstance();
        //when recursion proxy , reverse and process  to get the origin
        List<BeanPostProcessor> reversedProcessors=new ArrayList<>(beanPostProcessors);
        Collections.reverse(reversedProcessors);
        for(BeanPostProcessor beanPostProcessor:reversedProcessors){
            Object originInstance=beanPostProcessor.postProcessOnSetProperty(instance,def.getName());
            if(originInstance!=instance){
                log.info("BeanPostProcessor {} specified injection from {} to {}.", beanPostProcessor.getClass().getSimpleName(),
                        instance.getClass().getSimpleName(), originInstance.getClass().getSimpleName());
                instance=originInstance;
            }
        }
        return instance;
    }
    void injectBean(BeanDef beanDef,Object targetInstance,Class<?> clazz,AccessibleObject fieldOrSetter) throws IllegalAccessException, InvocationTargetException {
        Value value=fieldOrSetter.getAnnotation(Value.class);
        Autowired autowired=fieldOrSetter.getAnnotation(Autowired.class);
        //nothing inject
        if (value == null && autowired == null) {
            return;
        }
        Field field=null;
        Method method=null;
        if(fieldOrSetter instanceof Field f){
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field=f;
        }
        if(fieldOrSetter instanceof Method m){
            checkFieldOrMethod(m);
            m.setAccessible(true);
            method=m;
        }
        String name=null;
        Class<?> type=null;
        if(field!=null){
            name=field.getName();
            type=field.getType();
        }else if(method!=null){
            name=method.getName();
            type=method.getParameterTypes()[0];
        }
        if(value!=null&&autowired!=null){
            throw new IocException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), name, beanDef.getName(), beanDef.getBeanClass().getName()));
        }
        //@Value
        if(value!=null){
            Object instance=this.propertyResolver.getProperty(value.value(),type);
            if(field!=null){
                field.set(targetInstance,instance);
            }else{
                method.invoke(targetInstance,instance);
            }
        }
        //@Autowired
        if(autowired!=null){
            String beanName= autowired.name();
            boolean required=autowired.value();
            Object instance=null;
            if(beanName.isEmpty()){
                instance=findBean(type);
            }else{
                instance=findBean(beanName,type);
            }
            if(required&&instance==null){
                throw  new IocException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        name, beanDef.getName(), beanDef.getBeanClass().getName()));
            }
            if(instance!=null){
                if(field!=null){
                    field.set(targetInstance,instance);
                }else if(method!=null){
                    method.invoke(targetInstance,instance);
                }
            }
        }


    }
    void checkFieldOrMethod(Member member){
        int mod=member.getModifiers();
        if(Modifier.isStatic(mod)){
            throw  new IocException("Cannot inject static field: " + member);
        }
        if(Modifier.isFinal(mod)){
            if(member instanceof Field field){
                throw new IocException( "Cannot inject final field: " + field);
            }else if(member instanceof  Method method){
                //setter just one parameter
                if(method.getParameterCount()!=1){
                    throw new IocException(String.format("Cannot inject a non-setter method %s", method.getName()));
                }
            }
        }
    }



    /**
     * create @Configuration factory bean
     */
    void createConfigurationFactoryBeans(){
        beans.values().stream().filter(def->this.isConfugurationBean(def)).map(
                def->{
                    createSingletonBean(def);
                    return def;
                });
    }
    /**
     * create normal bean
     */
    void createNormalBeans(){
        beans.values().forEach(def->{
            if(def.getInstance()==null){
                createSingletonBean(def);
            }
        });
    }

    /**
     * no  setter&field inject
     * @param def
     */
    public Object createSingletonBean(BeanDef def){
        log.info("Try create bean '{}' as  singleton: {}", def.getName(), def.getBeanClass().getName());
        if(!this.creatingBeanNames.add(def.getName())){
            throw new IocException(String.format("Strong Circular dependency detected when create bean '%s'", def.getName()));
        }
        Executable createFun=null;
        if(def.getFactoryName()==null){
            createFun=def.getConstructor();
        }else{
            createFun=def.getFactoryMethod();
        }
        Parameter[] parameters=createFun.getParameters();
        Annotation[][] parameterAnnos=createFun.getParameterAnnotations();
        Object[] args=new Object[parameters.length];
        //create paramter instances
        for(int i=0;i<parameters.length;i++){
            Parameter parameter=parameters[i];
            Annotation[] annotations=parameterAnnos[i];
            Value value=ClassUtils.findAnnotation(annotations,Value.class);
            Autowired autowired=ClassUtils.findAnnotation(annotations,Autowired.class);
            // Configuration constructor can't use @autowired
            if(isConfugurationBean(def)&&autowired!=null){
                throw new IocException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            // parameters need @Value/@Autowired
            if (value != null && autowired != null) {
                throw new IocException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new IocException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            Class type=parameter.getType();
            if(value!=null){
                args[i]=this.propertyResolver.getProperty(value.value(),type);
            }else{
                //Autowired
                String name= autowired.name();
                Boolean isrequired=autowired.value();
                BeanDef beanDef=null;
                if(name.isEmpty()){
                    beanDef=findBeanDef(type);

                }else{
                    beanDef=findBeanDef(name);
                }
                if(beanDef==null&&isrequired){
                    throw new IocException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }
                if(beanDef!=null){
                    Object paraminstance=beanDef.getInstance();
                    if(paraminstance==null){
                        args[i]=createSingletonBean(beanDef);
                    }else{
                        args[i]=paraminstance;
                    }
                }else{
                    args[i]=null;
                }
            }

        }
        //create baen instance
        Object instance =null;
        if(def.getFactoryName()==null){
            try {
                instance=def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new IocException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }else{
            Object factoryInstance =getBean(def.getFactoryName());
            try {
                instance=def.getFactoryMethod().invoke(factoryInstance,args);
            } catch (Exception e) {
                throw new IocException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }

        //call match BeanPostProcessor
        for(BeanPostProcessor processor:beanPostProcessors){
            Object convertInstance=processor.postProcessBeforeInitialization(instance,def.getName());
            if(convertInstance==null){
                throw  new IocException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), processor));

            }
            //match
            if(instance!=convertInstance){
                log.info("Bean '{}' was replaced by post processor {}.", def.getName(), processor.getClass().getName());
                def.setInstance(convertInstance);
            }
        }
        def.setInstance(instance);
        return instance;


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
    public Map<String, BeanDef> createBeanDefs(Set<String> classNameSet){
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
                    scanFactoryMethods(beanName,clazz,beans);
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
     * get BeanDef by name & type
     * check type match
     * @param name
     * @param type
     * @return
     */
    @Nullable
    public BeanDef findBeanDef(String name,Class <?> type){
        BeanDef beanDef=beans.get(name);
        if(beanDef==null){
            return null;
        }else if(!type.isAssignableFrom(beanDef.getBeanClass())){
            throw  new IocException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", type.getName(),
                    name, beanDef.getBeanClass().getName()));
        }
        return beanDef;
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
     * get BeanDefs by type
     * @param clazz
     * @return
     */
    public List<BeanDef> findBeanDefs(Class<?> clazz){
        List<BeanDef> rs=beans.values().stream().filter(def->
            clazz.isAssignableFrom(def.getBeanClass())
        ).collect(Collectors.toList());
        return rs;
    }

    public boolean isConfugurationBean(BeanDef beanDef){
        return ClassUtils.findAnnotation(beanDef.getBeanClass(),Configuration.class)!=null;
    }
    boolean isBeanPostProcessorDef(BeanDef def){
        return  BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    /**
     * get factory bean instance by name
     * not exist throw exception
     * @param name
     * @param <T>
     * @return
     */
    public <T> T getBean(String name){
        BeanDef beanDef=this.beans.get(name);
        if(beanDef==null){
            throw new IocException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) beanDef.getInstance();
    }

    /**
     * get bean instance by type
     * @param type
     * @return
     * @param <T>
     */
    @Nullable
    public <T> T findBean(Class<T>  type){
        BeanDef beanDef=findBeanDef(type);
        if(beanDef==null){
            return null;
        }
        return (T) beanDef.getInstance();
    }

    /**
     * get bean instance by type & name
     *
     * @param name
     * @param type
     * @return
     * @param <T>
     */
    @Nullable
    public <T> T findBean(String  name,Class<T>  type){
        BeanDef beanDef=findBeanDef(name,type);
        if(beanDef==null){
            return null;
        }
        return (T) beanDef.getInstance();
    }

    @Nullable
    public <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefs(requiredType).stream().map(def -> (T) def.getRequiredInstance()).collect(Collectors.toList());
    }

    /**
     * invoke @PostConstruct or @PreDestroy method
     * @param instance
     * @param method
     * @param methodName
     */
    void invokeMethod(Object instance,Method method,String methodName){
        if(method!=null){
            try {
                method.invoke(instance);
            } catch (ReflectiveOperationException e) {
                throw new IocException(e);
            }
        }else if(methodName!=null){
            try {
                Method namedMethod=instance.getClass().getDeclaredMethod(methodName);
                namedMethod.setAccessible(true);
                namedMethod.invoke(instance);
            } catch (ReflectiveOperationException e) {
                throw new IocException(e);
            }
        }
    }
    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }
    public void close(){
        this.beans.values().forEach(def->{
            Object instance=getProxiedInstance(def);
            invokeMethod(instance,def.getDestroyMethod(),def.getDestroyMethodName());
        });
        this.beans.clear();
        ApplicationContextUtils.setApplicationContext(null);
    }


}
