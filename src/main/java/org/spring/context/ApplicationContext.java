package org.spring.context;

import org.spring.annotation.AutoWired;
import org.spring.config.BeanDefinition;
import org.spring.config.Const;
import org.spring.annotation.Component;
import org.spring.annotation.ComponentScan;
import org.spring.annotation.Scope;
import org.spring.factory.BeanNameAware;
import org.spring.factory.InitializingBean;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    private Class<?> configClass;

    public void setConfigClass(Class<?> configClass) {
        this.configClass = configClass;
    }

    private final ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    public ApplicationContext(Class<?> configClass) {
        this.configClass = configClass;

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

    public Object createBean(String beanName, BeanDefinition beanDefinition) {
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

            // 初始化
            if (bean instanceof InitializingBean) {
                try {
                    ((InitializingBean) bean).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

                    BeanDefinition beanDefinition = new BeanDefinition(clazz, Const.SINGLETON);
                    if (clazz.isAnnotationPresent(Scope.class)) {
                        Scope scope = clazz.getDeclaredAnnotation(Scope.class);
                        beanDefinition.setScope(scope.value());
                    }
                    beanDefinitionMap.put(beanName, beanDefinition);

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
            return singletonObjects.get(beanName);
        }
        return createBean(beanName, beanDefinition);
    }

}
