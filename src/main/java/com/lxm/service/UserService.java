package com.lxm.service;

import org.spring.annotation.AutoWired;
import org.spring.annotation.Component;
import org.spring.factory.BeanNameAware;
import org.spring.factory.InitializingBean;

@Component("userService")
public class UserService implements BeanNameAware, InitializingBean, IUserService {

    @AutoWired
    private OrderService orderService;

    private String beanName;

    public void test() {
        System.out.println(beanName);
        orderService.show();
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("初始化方法");
    }
}
