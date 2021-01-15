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
package com.entdiy.nat.client.config;

import com.entdiy.nat.common.model.Tunnel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "nat")
public class NatClientConfigProperties {
    private Boolean idlePingEnabled = Boolean.TRUE;
    private String client;
    private String secret;
    private String version;
    private String mmVersion;

    private int reconnectSeconds = 10;
    private String serverAddr;
    private int port = -1;
    private String domain;

    private Integer poolCoreSize;
    private Integer poolIdleSize;
    private Integer poolMaxSize;

    private String tunnelsMode;
    private Map<String, Tunnel> tunnels;


}
