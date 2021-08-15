package com.lxm.service;

import org.spring.AutoWired;
import org.spring.Const;
import org.spring.annotation.Component;
import org.spring.annotation.Scope;

@Component("userService")
public class UserService {

    @AutoWired
    private OrderService orderService;

    public void test() {
        orderService.show();
    }

}
