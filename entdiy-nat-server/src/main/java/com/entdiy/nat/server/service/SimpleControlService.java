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
package com.entdiy.nat.server.service;

import com.entdiy.nat.common.model.AuthMessage;
import com.entdiy.nat.server.ServerContext;
import com.entdiy.nat.server.config.NatServerConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SimpleControlService implements ControlService {

    private static final Map<String, String> authDataMapping = new ConcurrentHashMap<>();

    @Override
    public String authClient(AuthMessage authMessage) {
        //注意安全风险：忽略账号密码验证，直接返回可用clientId值
        String client = authMessage.getClient();
        NatServerConfigProperties config = ServerContext.getConfig();
        NatServerConfigProperties.Client theClient = config.getClients().get(client);
        if (theClient == null
                || !StringUtils.hasText(theClient.getSecret())
                || !StringUtils.hasText(authMessage.getSecret())
                || !theClient.getSecret().equals(authMessage.getSecret())) {
            return null;
        }
        String clientToken = UUID.randomUUID().toString();
        //最后登录会覆盖之前token，相当于后面客户端连接会自动把之前登录会话踢掉
        authDataMapping.put(client, clientToken);
        return clientToken;
    }

    @Override
    public String validateClientToken(String clientToken) {
        for (Map.Entry<String, String> me : authDataMapping.entrySet()) {
            if (me.getValue().equals(clientToken)) {
                String client = me.getKey();
                log.debug("Validate success token for client: {} for token: {}", client, clientToken);
                return client;
            }
        }
        throw new IllegalArgumentException("Client token validate failure: " + clientToken);
    }
}
