package com.entdiy.nat.common.model;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class ProxyingMessage extends BaseResponseMessage {
    // URL of the tunnel this connection connection is being proxied for
    private String url;
    // Network address of the client initiating the connection to the tunnel
    private String clientAddr;

    private ByteBuf data;
}
