# Boot Design
## Features
support embed tomcat 
## set up 
start tomcat server  
servlet container set up listener   
    1.set up Ioc container   
    2.set up DispatcherServlet
## how to use it
```java
@ComponentScan//scan its pkg
@Import({ WebMvcConfiguration.class })
public class HelloApplication{
    public static void main(String[] args) {
        try {
            YSpringApplication.run("src/main/webapp","target/classes",HelloApplication.class,args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
```