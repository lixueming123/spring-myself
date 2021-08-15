package com.lxm.aop;

import org.spring.annotation.aop.After;
import org.spring.annotation.aop.Aspect;
import org.spring.annotation.Component;
import org.spring.annotation.aop.Before;
import org.spring.annotation.aop.PointCnt;

@Aspect
@Component("enhance")
public class Enhance {

    @PointCnt(AopGo.class)
    public void pointCut() {}

    @Before("pointCut()")
    public void before() {
        System.out.println("Enhance.before()");
    }

    @After("pointCut()")
    public void after() {
        System.out.println("Enhance.after()");
    }
}
