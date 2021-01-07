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
package com.entdiy.nat.client;

import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.client.constant.TunnelsModeEnum;
import com.entdiy.nat.client.listener.NatClientListener;
import com.entdiy.nat.common.model.Tunnel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties(NatClientConfigProperties.class)
@Slf4j
public class NatClientApplication {

    @Autowired
    private NatClientConfigProperties config;

    private NatClientListener natClientListener = new NatClientListener();


    public static void main(String[] args) {
        SpringApplication.run(NatClientApplication.class, args);
    }

    @PostConstruct
    public void init() {
        ClientContext.setConfig(config);
        log.info("Running at {} TunnelsMode", config.getTunnelsMode());
        if (!TunnelsModeEnum.server.equals(config.getTunnelsMode())) {
            Map<String, Tunnel> tunnels = config.getTunnels();
            Assert.isTrue(tunnels != null && tunnels.size() > 0, "nat.tunnels config required");
        }
        natClientListener.run();
    }

    @PreDestroy
    public void shutdown() {
        natClientListener.shutdown();
    }
}
