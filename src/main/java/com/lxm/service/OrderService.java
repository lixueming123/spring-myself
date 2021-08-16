package com.lxm.service;

import com.lxm.aop.AopGo;
import org.spring.annotation.Component;

@Component
public class OrderService {
    public void show() {
        System.out.println("orderService show()");
    }

    @AopGo
    public void test() {
        System.out.println("orderService.test()");
    }
}
