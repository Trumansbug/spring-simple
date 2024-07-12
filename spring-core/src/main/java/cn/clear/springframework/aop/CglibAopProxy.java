package cn.clear.springframework.aop;


import cn.clear.springframework.aop.support.AdvisedSupport;

public class CglibAopProxy implements AopProxy {

    public CglibAopProxy(AdvisedSupport config) {
    }

    @Override
    public Object getProxy() {
        return null;
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        return null;
    }
}
