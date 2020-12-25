/**
 * Copyright @ 2020-2020 EntDIY-NAT (like Ngrok) based on Netty
 *
 * Author: Li Xia, E-Mail: xautlx@hotmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.entdiy.nat.server;

import com.entdiy.nat.server.config.NatServerConfigProperties;
import com.entdiy.nat.server.listener.NatServerListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableConfigurationProperties(NatServerConfigProperties.class)
public class NatServerApplication implements ApplicationContextAware {

    @Autowired
    private NatServerConfigProperties config;

    private NatServerListener natServerListener = new NatServerListener();

    public static void main(String[] args) {
        SpringApplication.run(NatServerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        ServerContext.setConfig(config);
        natServerListener.run();
    }

    @PreDestroy
    public void shutdown() {
        natServerListener.shutdown();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ServerContext.setApplicationContext(applicationContext);
    }
}
