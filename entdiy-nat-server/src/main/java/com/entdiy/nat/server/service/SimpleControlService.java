/**
 * Copyright @ 2020-2020 EntDIY NAT (like Ngrok) based on Netty
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
import org.apache.tomcat.util.codec.binary.Base64;

public class SimpleControlService implements ControlService {


    @Override
    public String authClient(AuthMessage authMessage) {
        //注意安全风险：忽略账号密码验证，直接返回可用clientId值
        return Base64.encodeBase64String(authMessage.getClientId().getBytes());
    }

    @Override
    public String validateClientToken(String clientToken) {
        return new String(Base64.decodeBase64(clientToken));
    }
}
