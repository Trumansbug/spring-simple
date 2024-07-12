package cn.clear;

import cn.clear.demo.AppConfig;
import cn.clear.demo.Hello.IHello;
import cn.clear.springframework.context.support.AnnotationConfigApplicationContext;
import cn.clear.springframework.context.support.DefaultApplicationContext;
import cn.clear.springframework.core.factory.ApplicationContext;

/**
 * Hello world!
 *
 */
public class SpringDemo 
{
    public static void main( String[] args ) throws Exception {
//        ApplicationContext applicationContext = new DefaultApplicationContext("application.properties");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        IHello hello = (IHello) applicationContext.getBean("helloService");
        hello.sayHello();
    }
}
