package org.spring;


import com.lxm.config.AppConfig;
import com.lxm.service.IUserService;
import com.lxm.service.OrderService;
import com.lxm.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.spring.context.ApplicationContext;

public class AppTest {

    private ApplicationContext applicationContext;

    @Before
    public void init() {
        this.applicationContext = new ApplicationContext(AppConfig.class);
    }

    @Test
    public void test() {
        IUserService userService = applicationContext.getBean("userService", UserService.class);
        userService.test();

        OrderService orderService = applicationContext.getBean("orderService", OrderService.class);
        orderService.test();

    }

}
