/*
 * Copyright (c) 2021-2022, jad (cxxwl96@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cxxwl96.spring.context;

import com.cxxwl96.spring.annotation.Autowired;
import com.cxxwl96.spring.annotation.Component;
import com.cxxwl96.spring.annotation.ComponentScan;
import com.cxxwl96.spring.annotation.Scope;
import com.cxxwl96.spring.bean.BeanDefinition;
import com.cxxwl96.spring.config.AppConfig;
import com.cxxwl96.spring.interfaces.BeanNameAware;
import com.cxxwl96.spring.interfaces.BeanPostProcessor;
import com.cxxwl96.spring.interfaces.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.util.StrUtil;

/**
 * Spring上下文
 *
 * @author cxxwl96
 * @since 2022/9/5 22:12
 */
public class SpringContext {
    // bean定义的map池
    private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    // 单例bean对象池
    private final ConcurrentHashMap<String, Object> beanSingletonMap = new ConcurrentHashMap<>();

    // bean处理器列表
    private final List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public SpringContext(Class<? extends AppConfig> configClazz) {
        // 扫描包
        if (configClazz.isAnnotationPresent(ComponentScan.class)) {
            // 得到扫描包路径
            String packageName = configClazz.getAnnotation(ComponentScan.class).value();
            // 转换包名为路径
            packageName = packageName.replace(".", "/");
            final URL url = SpringContext.class.getClassLoader().getResource(packageName);
            final URL baseUrl = SpringContext.class.getClassLoader().getResource("");
            Objects.requireNonNull(url, "Invalid URL: " + url);
            Objects.requireNonNull(baseUrl, "Invalid URL: " + baseUrl);
            // 遍历该路径下所有class文件
            final File file = new File(url.getFile());
            loopScanClass(baseUrl.getFile(), file);
        }
    }

    private void loopScanClass(String baseUrlFile, File file) {
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File item : files) {
                loopScanClass(baseUrlFile, item);
            }
        }
        if (!file.getName().endsWith(".class")) {
            return;
        }
        String absolutePath = file.getAbsolutePath();
        absolutePath = absolutePath.replace(baseUrlFile, "");
        absolutePath = absolutePath.substring(0, absolutePath.indexOf(".class"));
        // 得到类名（com.cxxwl96.test.UserService）
        String className = absolutePath.replace("/", ".");
        try {
            final Class<?> clazz = SpringContext.class.getClassLoader().loadClass(className);
            if (!clazz.isAnnotationPresent(Component.class)) {
                return;
            }
            // 是否是bean处理器
            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                beanPostProcessorList.add((BeanPostProcessor) clazz.newInstance());
            }
            // 得到beanName
            String beanName = clazz.getAnnotation(Component.class).value();
            if (StrUtil.isBlank(beanName)) {
                // 默认beanName为类名小驼峰
                final String name = className.substring(className.lastIndexOf(".") + 1);
                beanName = Introspector.decapitalize(name);
            }
            // 默认scope为singleton
            String scope = "singleton";
            if (clazz.isAnnotationPresent(Scope.class)) {
                final String value = clazz.getAnnotation(Scope.class).value();
                if ("prototype".equals(value)) {
                    scope = "prototype";
                }
            }
            // 存入beanDefinitionMap
            final BeanDefinition beanDefinition = new BeanDefinition().setClazz(clazz).setScope(scope);
            beanDefinitionMap.put(beanName, beanDefinition);
            // 如果是单例则创建bean存入beanSingletonMap中
            if ("singleton".equals(scope)) {
                Object bean = createBean(beanName, beanDefinition);
                beanSingletonMap.put(beanName, bean);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        try {
            final Class<?> clazz = beanDefinition.getClazz();
            final Object instance = clazz.newInstance();
            // Autowired依赖注入
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                final boolean accessible = field.isAccessible();
                field.setAccessible(true);
                // 变量名注入
                final String fieldName = field.getName();
                field.set(instance, getBean(fieldName));
                field.setAccessible(accessible);
            }
            // BeanName回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }
            // bean前置处理
            beanPostProcessorList.forEach(
                beanPostProcessor -> beanPostProcessor.postProcessBeforeInitialization(beanName, instance));
            // 初始化bean属性设置
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }
            // bean后置处理
            beanPostProcessorList.forEach(
                beanPostProcessor -> beanPostProcessor.postProcessAfterInitialization(beanName, instance));
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getBean(String beanName) {
        final BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        Objects.requireNonNull(beanDefinition, beanName + " is not defined");
        if ("singleton".equals(beanDefinition.getScope())) {
            Object bean = beanSingletonMap.get(beanName);
            if (bean == null) {
                bean = createBean(beanName, beanDefinition);
                beanSingletonMap.put(beanName, bean);
            }
            return bean;
        }
        return createBean(beanName, beanDefinition);
    }
}
