package cn.clear.springframework.beans;

/**
 * 当 BeanDefinition 的 Bean 配置信息被读取并实例化成一个实例后，这个实例封装在 BeanWrapper 中
 */
public class BeanWrapper {
    /**
     * Bean的实例化对象
     */
    private Object wrappedObject;

    public BeanWrapper(Object wrappedObject) {
        this.wrappedObject = wrappedObject;
    }

    public Object getWrappedInstance() {
        return this.wrappedObject;
    }

    public Class<?> getWrappedClass() {
        return getWrappedInstance().getClass();
    }
}
