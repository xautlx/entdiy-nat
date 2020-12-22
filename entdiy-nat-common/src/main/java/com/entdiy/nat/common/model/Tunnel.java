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
