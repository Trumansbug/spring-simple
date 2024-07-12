package cn.clear.springframework.webmvc.servlet;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
public class HandlerMapping {

    /**
     * 保存方法对应的实例
     */
    private Object controller;
    
    /**
     * 保存映射的方法
     */
    private Method method;
    
    /**
     * URL的正则匹配
     */
    private Pattern pattern;
}
