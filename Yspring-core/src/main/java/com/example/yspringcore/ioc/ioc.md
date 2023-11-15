# IoC Design
## dependency injection
### Strong dependence
A bean instance  creatation depends B dependency injection  
eg.  A strong depends B ->  A(B b)   
constructer  parameter  dependency injection
````
@Component
public class A {
    B b;
    public A(@Autowired B b) {
        this.B= b;
    }
}
````
factory method  parameter   dependency injection
````
@Configuration
public class XConfig {
    @Bean
    A a(@Autowired B b) {
        return new A(b);
    }
}
````
<font color=red>circular dependency  Unsolvable</font>
A(B b)  B(A a)
### weak dependence
create A bean instance first then B dependency injection through reflection  
eg.  A weak depends B  
setter method    dependency injection
````
@Component
public class A {
    B b;
    @Autowired 
    public setB(B b) {
        this.B= b;
    }
}
````
field   dependency injection
````
@Component
public class A {
    @Autowired 
    B b;
}
````
<font color=red>circular dependency  solvable</font>
A()  B() a.b=b b.a=a

@Bean创建的不是单例


