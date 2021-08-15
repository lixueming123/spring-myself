package org.spring.config;

public class BeanDefinition {

    private Class<?> beanClass;
    private String scope;

    public BeanDefinition(Class<?> beanClass, String scope) {
        this.beanClass = beanClass;
        this.scope = scope;
    }

    public BeanDefinition() {

    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
