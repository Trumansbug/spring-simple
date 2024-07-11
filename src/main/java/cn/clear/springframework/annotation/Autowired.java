package cn.clear.springframework.annotation;

import java.lang.annotation.*;

/**
 * 依赖注入注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
