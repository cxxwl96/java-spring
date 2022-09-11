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

package com.cxxwl96.test;

import com.cxxwl96.spring.annotation.Component;
import com.cxxwl96.spring.interfaces.BeanPostProcessor;

/**
 * 自定义bean处理器
 *
 * @author cxxwl96
 * @since 2022/9/7 23:38
 */
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public void postProcessBeforeInitialization(String beanName, Object bean) {
        if (bean instanceof UserService) {
            System.out.println("postProcessBeforeInitialization: beanName=" + beanName + " bean=" + bean);
        }
    }

    @Override
    public void postProcessAfterInitialization(String beanName, Object bean) {
        if (bean instanceof UserService) {
            System.out.println("postProcessAfterInitialization: beanName=" + beanName + " bean=" + bean);
        }
    }
}
