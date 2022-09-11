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

import com.cxxwl96.spring.annotation.Autowired;
import com.cxxwl96.spring.annotation.Component;
import com.cxxwl96.spring.annotation.Scope;
import com.cxxwl96.spring.interfaces.BeanNameAware;
import com.cxxwl96.spring.interfaces.InitializingBean;

import lombok.Data;

/**
 * UserService
 *
 * @author cxxwl96
 * @since 2022/9/6 22:26
 */
@Data
@Scope("prototype")
@Component
public class UserService implements BeanNameAware, InitializingBean {
    @Autowired
    private OrderService orderService;

    private String beanName;

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("afterPropertiesSet...");
    }
}
