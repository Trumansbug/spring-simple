package cn.clear.demo;

import cn.clear.springframework.annotation.Autowired;
import cn.clear.springframework.annotation.Component;
import cn.clear.springframework.annotation.Service;

@Component
public class TestUtil {
    
    @Autowired
    TestUtil2 testUtil2;
    
    
    
    public void test() {
        testUtil2.test();
        System.err.println("这是一个Test方法");
    }
}
