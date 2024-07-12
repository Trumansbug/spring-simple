package cn.clear.demo.Hello.impl;

import cn.clear.demo.Hello.IHello;
import cn.clear.demo.TestUtil;
import cn.clear.springframework.annotation.Autowired;
import cn.clear.springframework.annotation.Service;

@Service
public class HelloService implements IHello {
    
    @Autowired
    TestUtil testUtil;
    
    
    public void sayHello() {
        testUtil.test();
        System.err.println("Hello World!!!!");
    }
}
