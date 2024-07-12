package cn.clear.springframework.context.support;

import cn.clear.springframework.annotation.Autowired;
import cn.clear.springframework.aop.AopProxy;
import cn.clear.springframework.aop.CglibAopProxy;
import cn.clear.springframework.aop.JdkDynamicAopProxy;
import cn.clear.springframework.aop.config.AopConfig;
import cn.clear.springframework.aop.support.AdvisedSupport;
import cn.clear.springframework.beans.BeanWrapper;
import cn.clear.springframework.beans.config.BeanDefinition;
import cn.clear.springframework.beans.support.BeanDefinitionReader;
import cn.clear.springframework.core.factory.ApplicationContext;
import cn.clear.springframework.util.StringUtil;
import cn.clear.springframework.webmvc.servlet.ViewResolver;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultApplicationContext implements ApplicationContext {
    /**
     * 配置文件路径
     */
    private String configLocation;

    BeanDefinitionReader reader;

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    private Map<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();


    public DefaultApplicationContext(String configLocation) {
        this.configLocation = configLocation;
        try {
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refresh() throws Exception {
        // 1、定位配置文件
        reader = new BeanDefinitionReader(this.configLocation);

        // 2、加载配置文件，扫描相关的类，把它们封装成BeanDefinition
        List<BeanDefinition> beanDefinitions = reader.loadBeanDefinitions();

        // 3、注册，把配置信息放到容器里面(伪IOC容器)
        doRegisterBeanDefinition(beanDefinitions);

        // 4、把不是延时加载的类，提前初始化
        doAutowired();
    }

    @Override
    public Object getBean(String beanName) throws Exception {
        // 已经加载到容器了，直接返回
        Object instance = getSingleton(beanName);
        if (instance != null) {
            return instance;
        }

        // 获取bean的定义
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        // 创建该bean的一个实例
        instance = instantiateBean(beanName, beanDefinition);

        // 封装成beanWrapper
        BeanWrapper beanWrapper = new BeanWrapper(instance);
        
        // 添加到容器中，bean的名称
        factoryBeanInstanceCache.put(beanName, beanWrapper);
        // 添加到容器中，bean的类路径
        factoryBeanInstanceCache.put(beanDefinition.getBeanClassName(), beanWrapper);
        
        // 依赖注入
        populateBean(beanName, beanDefinition, beanWrapper);

        // 返回完成的bean
        return factoryBeanInstanceCache.get(beanName).getWrappedInstance();
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        return (T) getBean(requiredType.getName());
    }
    
    private void doRegisterBeanDefinition(List<BeanDefinition> beanDefinitions) throws Exception {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception("The \"" + beanDefinition.getFactoryBeanName() + "\" is exists!!");
            }
            beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
        }
    }

    /**
     * 加载非延迟加载的bean
     */
    private void doAutowired() {
        // 遍历扫描到的bean定义，进行初始化
        for (Map.Entry<String, BeanDefinition> beanDefinitionEntry : beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            // 加载非懒加载的bean
            if (!beanDefinitionEntry.getValue().isLazyInit()) {
                try {
                    getBean(beanName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取bean的一个实例
     * @param beanName bean的名称或者类路径
     * @return bean的实例
     */
    private Object getSingleton(String beanName) {
        BeanWrapper beanWrapper = factoryBeanInstanceCache.get(beanName);
        return beanWrapper == null ? null : beanWrapper.getWrappedInstance();
    }

    /**
     * 初始化bean
     * @param beanName bean的名称
     * @param beanDefinition bean的定义
     * @return bean的实例
     */
    private Object instantiateBean(String beanName, BeanDefinition beanDefinition) {
        String beanClassName = beanDefinition.getBeanClassName();
        
        Object instance = null;
        try {
            Class<?> aClass = Class.forName(beanClassName);
            instance = aClass.newInstance();

            AdvisedSupport aopConfig = getAopConfig();
            if (aopConfig != null) {
                aopConfig.setTargetClass(aClass);
                aopConfig.setTarget(instance);

                if (aopConfig.pointCutMatch()) {
                    instance = createProxy(aopConfig).getProxy();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return instance;
    }

    /**
     * 依赖注入
     * @param beanName bean的名称
     * @param beanDefinition bean的定义
     * @param beanWrapper bean封装的wrapper对象
     */
    private void populateBean(String beanName, BeanDefinition beanDefinition, BeanWrapper beanWrapper) {
        // 获取bean的类信息
        Class<?> clazz = beanWrapper.getWrappedClass();
        
        // 获取该类所有的属性
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            // 如果属性上没有 @Autowired，不注入，跳过
            if (!field.isAnnotationPresent(Autowired.class)) {
                continue;
            }

            // 获取该属性上 Autowired 注解的信息
            Autowired autowired = field.getAnnotation(Autowired.class);
            // 获取注解设定的值
            String autowiredBeanName = autowired.value().trim();
            // 如果注解没有设定值，默认使用属性名称进行注入
            if (autowiredBeanName.isEmpty()) {
                autowiredBeanName = field.getType().getName();
            }
            
            // 强制访问该成员变量
            field.setAccessible(true);
            
            try {
                if (factoryBeanInstanceCache.get(autowiredBeanName) != null) {
                    continue;
                }

                // 将容器中的实例注入到成员变量中
                field.set(beanWrapper.getWrappedInstance(), factoryBeanInstanceCache.get(autowiredBeanName).getWrappedInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private AdvisedSupport getAopConfig() {
        String pointCut = this.reader.getConfig().getProperty("spring.aop.pointCut");
        String aspectClass = this.reader.getConfig().getProperty("spring.aop.aspectClass");
        if (StringUtil.isEmpty(pointCut) || StringUtil.isEmpty(aspectClass)) {
            return null;
        }
        
        AopConfig config = new AopConfig();
        config.setPointCut(pointCut);
        config.setAspectClass(aspectClass);
        config.setAspectBefore(this.reader.getConfig().getProperty("spring.aop.aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("spring.aop.aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("spring.aop.aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("spring.aop.aspectAfterThrowingName"));
        return new AdvisedSupport(config);
    }

    private AopProxy createProxy(AdvisedSupport config) {
        Class<?> targetClass = config.getTargetClass();
        // 如果接口数量大于0则使用JDK原生动态代理
        if (targetClass.getInterfaces().length > 0) {
            return new JdkDynamicAopProxy(config);
        }
        return new CglibAopProxy(config);
    }
    
    public String[] getBeanDefinitionNames() {
        return beanDefinitionMap.keySet().toArray(new String[0]);
    }

    public Properties getConfig() {
        return this.reader.getConfig();
    }
}
