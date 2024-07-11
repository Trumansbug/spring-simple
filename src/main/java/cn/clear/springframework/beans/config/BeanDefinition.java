package cn.clear.springframework.beans.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * bean 的配置信息
 */
@Getter
@Setter
@NoArgsConstructor
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
}
