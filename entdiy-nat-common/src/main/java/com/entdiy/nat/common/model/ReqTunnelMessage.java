package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class ReqTunnelMessage extends BaseRequestMessage {

    private String reqId;
    private String protocol;

    // HTTP only
    private String hostname;
    private String subdomain;
    private String httpAuth;

    // TCP only
    private Integer remotePort;

}
