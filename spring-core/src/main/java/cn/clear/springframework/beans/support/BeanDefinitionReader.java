package cn.clear.springframework.beans.support;

import cn.clear.springframework.annotation.Component;
import cn.clear.springframework.beans.config.BeanDefinition;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 扫描配置文件，解析成BeanDefinition
 */
public class BeanDefinitionReader {
    /**
     * 配置文件
     */
    @Getter
    private Properties config = new Properties();
    /**
     * 扫描包的配置key
     */
    private final String SCAN_PACKAGE = "spring.context.scan";
    /**
     * 存储扫描到的类的className
     */
    private List<String> registyBeanClasses = new ArrayList<>();
    
    public BeanDefinitionReader(String... locations) {
        try {
            // 找到配置文件，转换为文件流
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(locations[0].replace("classpath:", ""));
            
            // 加载
            config.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 扫描
        doScanner(config.getProperty(SCAN_PACKAGE));
    }
    
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        File listFile = new File(url.getFile());
        for (File file : listFile.listFiles()) {
            if (file.isDirectory()) {
                // 递归扫描
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                registyBeanClasses.add(className);
            }
        }
    }

    /**
     * 加载 bean 定义。
     *
     * @return 包含所有bean定义的列表。
     */
    public List<BeanDefinition> loadBeanDefinitions() {
        ArrayList<BeanDefinition> result = new ArrayList<>();
        try {
            for (String className : registyBeanClasses) {
                Class<?> beanClass = Class.forName(className);
                // 如果是接口，不能实例化，不封装
                if (beanClass.isInterface()) {
                    continue;
                }

                // 获取该类上的所有注解
                Annotation[] annotations = beanClass.getAnnotations();

                // 遍历注解
                for (Annotation annotation : annotations) {
                    // 获取注解类型
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    // 暂时只处理 @Component 注解的类
                    if (annotationType.isAnnotationPresent(Component.class)) {
                        result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()), beanClass.getName()));

                        // 获取该类实现的所有接口
                        Class<?>[] interfaces = beanClass.getInterfaces();
                        // 封装实现类与接口之间的关系
                        for (Class<?> anInterface : interfaces) {
                            result.add(doCreateBeanDefinition(anInterface.getName(), beanClass.getName()));
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        return result;
    }

    /**
     * 封装成BeanDefinition
     * @param factoryBeanName 工厂Bean名称
     * @param beanClassName bean Class名称
     * @return 封装好的BeanDefinition对象
     */
    private BeanDefinition doCreateBeanDefinition(String factoryBeanName, String beanClassName) {
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setFactoryBeanName(factoryBeanName);
        beanDefinition.setBeanClassName(beanClassName);
        return beanDefinition;
    }

    /**
     * 将类名的第一个字母转消协
     * @param simpleName 类名
     * @return 第一个字母转小写后的类名
     */
    private String toLowerFirstCase(String simpleName) {
        char[] charArray = simpleName.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
}
