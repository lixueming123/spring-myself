package org.spring.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JdkProxyFactory {

    public static Object getProxy(Object target) {
        return Proxy.newProxyInstance(
                JdkProxyFactory.class.getClassLoader(),
                target.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("代理逻辑=>before()");
                        Object invoke = method.invoke(target, args);
                        System.out.println("代理逻辑=>after()");
                        return invoke;
                    }
                }
        );
    }

}
