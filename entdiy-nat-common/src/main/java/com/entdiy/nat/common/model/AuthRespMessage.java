package com.entdiy.nat.common.model;

import lombok.Data;

import java.util.Map;

@Data
public class AuthRespMessage extends BaseResponseMessage {
    private String version;
    private String mmVersion;
    private String clientToken;

    /**
     * 服务端配置的穿透通道定义，可选属性
     */
    private Map<String, Tunnel> tunnels;
}
