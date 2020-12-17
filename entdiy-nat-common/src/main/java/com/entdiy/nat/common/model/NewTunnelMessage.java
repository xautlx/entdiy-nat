package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class NewTunnelMessage extends BaseResponseMessage{
    private String reqId;
    private String protocol;

    private String url;
}
