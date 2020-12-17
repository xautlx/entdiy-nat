package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class StartProxyMessage extends BaseResponseMessage{
    // Network address of the client initiating the connection to the tunnel
    private String clientAddr;

    // URL of the tunnel this connection connection is being proxied for
    private String url;
}
