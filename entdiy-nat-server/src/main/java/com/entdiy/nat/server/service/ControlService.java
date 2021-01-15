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
import org.springframework.stereotype.Service;

@Service
public interface ControlService {
    /**
     * 基于客户端认证信息校验其合法性并分配认证Token
     *
     * @param authMessage
     * @return
     */
    String authClient(AuthMessage authMessage);

    /**
     * 根据传入Token校验并返回对应的client标识信息
     * 校验失败抛出运行异常
     *
     * @param clientToken
     * @return 返回token对应的client值
     */
    String validateClientToken(String clientToken);
}
