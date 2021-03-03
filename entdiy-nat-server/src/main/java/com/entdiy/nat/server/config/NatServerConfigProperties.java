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
package com.entdiy.nat.server.config;

import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.SslUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@ConfigurationProperties(prefix = "nat")
public class NatServerConfigProperties {
    private Boolean sslAuth;
    private String keyStorePass;

    private String domain;
    private Integer httpAddr;
    private Integer tunnelAddr;

    private String version;
    private String mmVersion;

    private Map<String, Client> clients = new HashMap<>();

    private SSLEngine sslEngine;

    @Data
    public static class Client {
        private String secret;
        private List<Tunnel> tunnels;
    }

    @PostConstruct
    public void init() {
        if (!Boolean.FALSE.equals(getSslAuth())) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSLv3");
                KeyManager[] keyManagers = SslUtil.getKeyManagersServer("entdiy-nat-server.jks", getKeyStorePass());
                TrustManager[] trustManagers = SslUtil.getTrustManagersServer("entdiy-nat-server.jks", getKeyStorePass());
                if (keyManagers != null && trustManagers != null) {
                    sslContext.init(keyManagers, trustManagers, null);
                    sslContext.createSSLEngine().getSupportedCipherSuites();
                    sslEngine = sslContext.createSSLEngine();
                    sslEngine.setUseClientMode(false); //设置为服务端模式
                    sslEngine.setNeedClientAuth(true); //需要验证客户端
                    log.info("Running at SSL server mode");
                }
            } catch (Exception e) {
                log.error("SSL Error", e);
            }
        }
    }
}
