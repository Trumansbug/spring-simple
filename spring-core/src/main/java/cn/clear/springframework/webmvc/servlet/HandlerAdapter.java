package cn.clear.springframework.webmvc.servlet;


import cn.clear.springframework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 接收用户请求，将参数填充到Controller中的方法调用
 */
public class HandlerAdapter {

    /**
     * 处理请求并返回相应的 ModelAndView 对象。
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler 处理器对象，用于映射到具体的处理方法
     * @return ModelAndView 对象，包含处理结果和视图信息；如果处理方法返回 null，则此方法也返回 null
     * @throws Exception 如果处理过程中发生异常
     */
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMapping handlerMapping = (HandlerMapping) handler;

        // 把方法的形参列表和request的参数列表所在顺序进行一一对应。key：参数名称，value：参数在方法中的索引
        Map<String, Integer> paramIndexMapping = new HashMap<>();
        
        // 获取参数注解列表，因为一个方法可以有多个参数，一个参数可以有多个注解
        Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();
        for (int i = 0; i < pa.length; i++) {
            for (Annotation a : pa[i]) {
                if (a instanceof RequestParam) {
                    String paramName = ((RequestParam) a).value();
                    if (!paramName.trim().isEmpty()) {
                        paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }

        // 提取方法中的request和response参数
        Class<?>[] paramsTypes = handlerMapping.getMethod().getParameterTypes();
        for (int i = 0; i < paramsTypes.length; i++) {
            Class<?> type = paramsTypes[i];
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                paramIndexMapping.put(type.getName(), i);
            }
        }

        // 获取请求中的参数列表
        Map<String, String[]> params = request.getParameterMap();

        // controller的方法实参列表
        Object[] paramValues = new Object[paramsTypes.length];

        // 将controller中的参数和请求中的参数对应上
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", ",");

            // 如果方法上的参数列表中没有找到，继续
            if (!paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }

            // 获取请求参数对应方法参数的索引
            int index = paramIndexMapping.get(param.getKey());
            // 解析值，转换成对应的类型
            paramValues[index] = parseStringValue(value, paramsTypes[index]);
        }

        // 填充HttpServletRequest参数
        if (paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
        }

        // 填充HttpServletResponse参数
        if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
        }

        // 反射调用controller的方法
        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(), paramValues);
        // 方法执行后没有返回值，直接退出
        if (result == null) {
            return null;
        }

        // 解析controller的方法返回
        Class<?> returnType = handlerMapping.getMethod().getReturnType();
        boolean isModelAndView = returnType == ModelAndView.class;
        if (isModelAndView) {
            return (ModelAndView) result;
        } else if (returnType == Void.class) {
            return null;
        } else if (returnType == String.class) {
            // return (String) result;
        }

        return null;
    }

    /**
     * request中接收的参数都是string类型的，需要转换为controller中实际的参数类型
     * @param value 参数值
     * @param paramsType 参数类型
     */
    private Object parseStringValue(String value, Class<?> paramsType) {
        if (String.class == paramsType) {
            return value;
        }

        if (Integer.class == paramsType) {
            return Integer.valueOf(value);
        } else if (Double.class == paramsType) {
            return Double.valueOf(value);
        } else {
            return value;
        }
        
        // TODO 其他类型的实现
    }
}
