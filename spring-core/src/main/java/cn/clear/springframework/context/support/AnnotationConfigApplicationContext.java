package cn.clear.springframework.context.support;

import cn.clear.springframework.annotation.Autowired;
import cn.clear.springframework.annotation.Component;
import cn.clear.springframework.annotation.ComponentScan;
import cn.clear.springframework.beans.BeanWrapper;
import cn.clear.springframework.beans.config.BeanDefinition;
import cn.clear.springframework.core.factory.ApplicationContext;
import cn.clear.springframework.util.StringUtil;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationConfigApplicationContext implements ApplicationContext {
    /**
     * Bean名和它对应的BeanDefinition键值对
     */
    private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 单例池；map存各Bean名所对应的Bean实例
     */
    private final ConcurrentHashMap<String, BeanWrapper> singletonObjects = new ConcurrentHashMap<>();


    public AnnotationConfigApplicationContext(Class<?> config) throws Exception {
        // 解析配置类
        if (config.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = config.getAnnotation(ComponentScan.class);
            String path = componentScan.value();
            doScanner(path);
        }
        
        // 实例化bean
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            if (!entry.getValue().isLazyInit()) {
                getBean(beanName);
            }
        }
    }
    
    public void doScanner(String path) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(path.replace(".", "/"));
        File file = new File(url.getFile());
        for (File listFile : file.listFiles()) {
            if (listFile.isDirectory()) {
                doScanner(path + "." + listFile.getName());
            } else {
                if (listFile.getName().endsWith(".class")) {
                    String className = (path + "." + listFile.getName().replace(".class", ""));
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isInterface()) {
                        continue;
                    }
                    
                    Annotation[] annotations = clazz.getAnnotations();
                    for (Annotation annotation : annotations) {
                        Class<? extends Annotation> annotationType = annotation.annotationType();
                        Component component = null;
                        if (annotationType == Component.class) {
                            component = (Component) annotation;
                        } else if (annotationType.isAnnotationPresent(Component.class)) {
                            component = annotationType.getAnnotation(Component.class);
                        }
                        
                        if (component != null) {
                            String beanName = component.value().trim();
                            if (StringUtil.isEmpty(beanName)) {
                                beanName = Introspector.decapitalize(clazz.getSimpleName());
                            }

                            if (beanDefinitionMap.containsKey(beanName)) {
                                throw new Exception("Duplicate Bean '" + beanName + "'");
                            } else {
                                beanDefinitionMap.put(beanName, new BeanDefinition(clazz.getName(), beanName));
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    @Override
    public Object getBean(String name) throws Exception {
        // 已经加载了，直接返回
        if (singletonObjects.containsKey(name)) {
            return singletonObjects.get(name).getWrappedInstance();
        }
        
        // 创建bean
        return createBean(name);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        return null;
    }
    
    public Object createBean(String beanName) throws Exception {
        // 获取BeanDefinition
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new Exception("Bean " + beanName + " is not found");
        }
        String beanClassName = beanDefinition.getBeanClassName();
        
        // 创建容器
        Class<?> clazz = Class.forName(beanClassName);
        Object instance = clazz.newInstance();
        
        // 添加到容器中
        BeanWrapper beanWrapper = new BeanWrapper(instance);
        singletonObjects.put(beanName, beanWrapper);
        singletonObjects.put(beanDefinition.getBeanClassName(), beanWrapper);

        // 依赖注入
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Autowired autowired = field.getAnnotation(Autowired.class);
                String fieldBeanName = autowired.value().trim();
                if (StringUtil.isEmpty(fieldBeanName)) {
                    fieldBeanName = Introspector.decapitalize(field.getType().getSimpleName());
                }

                // 强制访问该成员变量
                field.setAccessible(true);

                field.set(instance, getBean(fieldBeanName));
            }
        }
        
        return instance;
    }
}
