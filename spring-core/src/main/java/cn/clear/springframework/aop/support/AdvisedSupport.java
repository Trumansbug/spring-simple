package cn.clear.springframework.aop.support;

import cn.clear.springframework.aop.aspect.AfterReturningAdviceInterceptor;
import cn.clear.springframework.aop.aspect.AfterThrowingAdviceInterceptor;
import cn.clear.springframework.aop.aspect.MethodBeforeAdviceInterceptor;
import cn.clear.springframework.aop.config.AopConfig;
import cn.clear.springframework.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvisedSupport {

    /**
     * 被代理的类class
     */
    @Getter
    private Class<?> targetClass;

    /**
     * 被代理的对象实例
     */
    @Getter
    @Setter
    private Object target;

    /**
     * 被代理的方法对应的拦截器集合
     */
    private Map<Method, List<Object>> methodCache;

    /**
     * AOP外部配置
     */
    private AopConfig config;

    /**
     * 切点正则表达式
     */
    private Pattern pointCutClassPattern;

    public AdvisedSupport(AopConfig config) {
        this.config = config;
    }

    /**
     * 获取拦截器
     */
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) throws Exception {
        List<Object> cached = methodCache.get(method);
        if (cached == null) {
            Method m = targetClass.getMethod(method.getName(), method.getParameterTypes());
            cached = methodCache.get(m);
            this.methodCache.put(m, cached);
        }

        return cached;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
        parse();
    }

    /**
     * 解析AOP配置，创建拦截器
     */
    private void parse() {
        //编译切点表达式为正则
        String pointCut = config.getPointCut()
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\\\.\\*", ".*")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)");
        String pointCutForClassRegex = pointCut.substring(0, pointCut.lastIndexOf("\\(") - 4);
        pointCutClassPattern = Pattern.compile("class " + pointCutForClassRegex.substring(
                pointCutForClassRegex.lastIndexOf(" ") + 1));

        try {
            // 保存切面的所有通知方法
            Map<String, Method> aspectMethods = new HashMap<>();
            // 获取切面类信息
            Class<?> aspectClass = Class.forName(this.config.getAspectClass());
            // 遍历切面类方法
            for (Method m : aspectClass.getMethods()) {
                aspectMethods.put(m.getName(), m);
            }

            // 遍历被代理类的所有方法，为符合切点表达式的方法创建拦截器
            methodCache = new HashMap<>();
            Pattern pattern = Pattern.compile(pointCut);
            for (Method m : this.targetClass.getMethods()) {
                String methodString = m.toString();
                // 为了能正确匹配这里去除函数签名尾部的throws xxxException
                if (methodString.contains("throws")) {
                    methodString = methodString.substring(0, methodString.lastIndexOf("throws")).trim();
                }

                Matcher matcher = pattern.matcher(methodString);
                if (matcher.matches()) {
                    // 执行器链
                    List<Object> advices = new LinkedList<>();

                    // 创建前置拦截器
                    if (config.getAspectBefore() != null && !config.getAspectBefore().isEmpty()) {
                        // 创建一个Advice
                        MethodBeforeAdviceInterceptor beforeAdvice = new MethodBeforeAdviceInterceptor(
                                aspectMethods.get(config.getAspectBefore()),
                                aspectClass.newInstance()
                        );
                        advices.add(beforeAdvice);
                    }
                    // 创建后置拦截器
                    if (config.getAspectAfter() != null && !config.getAspectAfter().isEmpty()) {
                        AfterReturningAdviceInterceptor returningAdvice = new AfterReturningAdviceInterceptor(
                                aspectMethods.get(config.getAspectAfter()),
                                aspectClass.newInstance()
                        );
                        advices.add(returningAdvice);
                    }
                    // 创建异常拦截器
                    if (config.getAspectAfterThrow() != null && !config.getAspectAfterThrow().isEmpty()) {
                        AfterThrowingAdviceInterceptor throwingAdvice = new AfterThrowingAdviceInterceptor(
                                aspectMethods.get(config.getAspectAfterThrow()),
                                aspectClass.newInstance()
                        );
                        throwingAdvice.setThrowName(config.getAspectAfterThrowingName());
                        advices.add(throwingAdvice);
                    }

                    // 保存被代理方法和执行器链的对应关系
                    methodCache.put(m, advices);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    
    /**
     * 判断一个类是否需要被代理 
     */
    public boolean pointCutMatch() {
        return pointCutClassPattern.matcher(this.targetClass.toString()).matches();
    }
}
