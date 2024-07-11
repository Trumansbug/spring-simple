package cn.clear.springframework.beans.support;

import lombok.Getter;

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
    private final String SCAN_PACKAGE = "scanPackage";
    
    public BeanDefinitionReader(String... locations) {
        
    }
}
