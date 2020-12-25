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
package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class Tunnel {
    /**
     * 穿透通道唯一编码标识，如：mysql
     */
    private String code;
    /**
     * 穿透通道描述名称，如：MySQL服务
     */
    private String name;
    private String subDomain;
    private String proto;
    private String host;
    private Integer port;
    private String schema;
    private Integer remotePort;

    /**
     * --------------------------
     *    以下为运行属性，无需配置
     * --------------------------
     */
    /**
     * 所属client标识
     */
    private String clientId;
    /**
     * 运行认证获取Token
     */
    private String clientToken;
}
