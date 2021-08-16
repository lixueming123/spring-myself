package org.spring.context;

import org.spring.annotation.AutoWired;
import org.spring.annotation.aop.Aspect;
import org.spring.annotation.aop.PointCnt;
import org.spring.config.BeanDefinition;
import org.spring.config.Const;
import org.spring.annotation.Component;
import org.spring.annotation.ComponentScan;
import org.spring.annotation.Scope;
import org.spring.factory.BeanNameAware;
import org.spring.factory.BeanPostProcessor;
import org.spring.factory.InitializingBean;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {

    private final Map<String,Object> singletonObjects = new ConcurrentHashMap<>(); // 单例池

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(); // bean定义Map

    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>(); // 后置处理器


    public ApplicationContext(Class<?> configClass) {

        // 初始化扫描，加入beanDefinitionMap中
        scan(configClass);

        // 创建所有单例bean
        createSingletonBeans();
    }

    private void createSingletonBeans() {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals(Const.SINGLETON)) {
                Object bean = singletonObjects.get(beanName);
                if (bean == null) {
                    bean = createBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, bean);
                }
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Object bean = null;
        try {
            bean = beanClass.getDeclaredConstructor().newInstance();
            // 依赖注入
            populateBean(beanName, bean, beanDefinition);

            // Aware回调
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanName);
            }

            // 初始化前
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
            }

            // 初始化
            if (bean instanceof InitializingBean) {
                try {
                    ((InitializingBean) bean).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 初始化后
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return bean;
    }

    private void populateBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        Field[] fields = beanDefinition.getBeanClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(AutoWired.class)) {
                field.setAccessible(true);
                try {
                    Object beanFiled;
                    if ((beanFiled = singletonObjects.get(field.getName())) == null) {
                         beanFiled = createBean(beanName, beanDefinitionMap.get(field.getName()));
                         singletonObjects.put(field.getName(), beanFiled);
                    }
                    field.set(bean, beanFiled);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void scan(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(ComponentScan.class)) {
            return;
        }
        ComponentScan componentScan = configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScan.value();
        path = path.replace(".", "/");
        ClassLoader classLoader = configClass.getClassLoader();
        URL resource = classLoader.getResource(path);
        assert resource != null;
        File file = new File(resource.getFile());
        List<String> classNames = getClassNames(file);
        for (String className : classNames) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz.isAnnotationPresent(Component.class)) {
                    Component component = clazz.getDeclaredAnnotation(Component.class);
                    String beanName = component.value();
                    if (beanName.equals("")) {
                        String packageName = clazz.getPackageName();
                        beanName = className.replace(packageName, "");
                        beanName = beanName.replace(".", "");
                        char[] chars = beanName.toCharArray();
                        chars[0] = Character.toLowerCase(chars[0]);
                        beanName = new String(chars);
                    }

                    BeanDefinition beanDefinition = new BeanDefinition(clazz, Const.SINGLETON);
                    if (clazz.isAnnotationPresent(Scope.class)) {
                        Scope scope = clazz.getDeclaredAnnotation(Scope.class);
                        beanDefinition.setScope(scope.value());
                    }
                    beanDefinitionMap.put(beanName, beanDefinition);

                    // BeanPostProcessor
                    if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                        BeanPostProcessor bean = (BeanPostProcessor) createBean(beanName,
                                beanDefinition);
                        beanPostProcessors.add(bean);
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    private List<String> getClassNames(File file) {
        List<String> classNames = new ArrayList<>();
        if (file == null) {
            return classNames;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f: files) {
                    classNames.addAll(getClassNames(f));
                }
            }
        } else {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".class")) {
                String path = file.getPath();
                String className = path.substring(
                        path.lastIndexOf("classes\\") + "classes\\".length()
                        , path.indexOf(".class"));
                className = className.replace("\\", ".");
                classNames.add(className);
            }
        }
        return classNames;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> beanClass) {
        assert beanClass != null;
        return (T) getBean(beanName);
    }

    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName))
            throw new NullPointerException("找不到一个 bean named '" + beanName + "'");

        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        String scope = beanDefinition.getScope();
        if (scope.equals(Const.SINGLETON)) {
            Object bean;
            if ((bean = singletonObjects.get(beanName)) == null) {
                bean = createBean(beanName, beanDefinition);
            }
            return bean;
        }
        return createBean(beanName, beanDefinition);
    }

}
