# Aop Design
## Features
only support annotation config  
only class proxy(not support interface)  
use ByteBuddy generate proxy class bytecode  
## How to use it
1. create before handler 
```
BeforeExampleHandler extends BeforeInvocationHandlerAdapter{
    @Override
    public void before(Object proxy, Method method, Object[] args) {
        xxx;
    }
}
```
2. inject AroundProxyBeanPostProcessor 
```
@Configuration
public class BeforeApplication {
    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
```
3. add annotation on the bean
```
@Component
@Around("beforeExampleHandler")
public class BusinessBean {


    public String proxiedMethod1() {
        return xxx;
    }

    public String proxiedMethod2(String name) {
        return xxx;
    }
}
```




