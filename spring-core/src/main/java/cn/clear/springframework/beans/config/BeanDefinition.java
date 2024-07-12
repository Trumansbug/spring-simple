package cn.clear.springframework.beans.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * bean 的配置信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeanDefinition {
    /**
     * 类名
     */
    private String beanClassName;
    /**
     * 懒加载
     */
    private boolean lazyInit = false;
    /**
     * 工厂bean名
     */
    private String factoryBeanName;
    
    public BeanDefinition(String beanClassName, String factoryBeanName) {
        this.beanClassName = beanClassName;
        this.factoryBeanName = factoryBeanName;
    }
}
