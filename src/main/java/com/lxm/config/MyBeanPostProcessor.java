package com.lxm.config;

import com.lxm.service.UserService;
import org.spring.annotation.Component;
import org.spring.factory.BeanPostProcessor;
import org.spring.factory.JdkProxyFactory;

@Component("myBeanPostProcessor")
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof UserService) {
            return JdkProxyFactory.getProxy(bean);
        }
        return bean;
    }
}
