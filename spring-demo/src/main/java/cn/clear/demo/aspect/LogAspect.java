package cn.clear.demo.aspect;

import cn.clear.springframework.aop.aspect.JoinPoint;

public class LogAspect {
    public void before(JoinPoint joinPoint) {
        System.err.println("前置通知");
    }
    
    public void after(JoinPoint joinPoint) {
        System.err.println("后置通知");
    }

    public void afterThrowing(JoinPoint joinPoint, Throwable ex){
        System.out.println(("出现异常，Throws:" + ex.getMessage()));
    }
}
