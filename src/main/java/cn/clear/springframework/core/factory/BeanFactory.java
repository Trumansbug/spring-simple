package cn.clear.springframework.core.factory;

/**
 * IOC 容器顶级父接口
 */
public interface BeanFactory {
    Object getBean(String name) throws Exception;
    
    <T> T getBean(Class<T> requiredType) throws Exception;
}
