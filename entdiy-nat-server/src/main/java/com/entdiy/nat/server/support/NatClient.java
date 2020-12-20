package com.entdiy.nat.server.support;

import lombok.Data;

@Data
public class NatClient {
    private String clientToken;
    private Integer poolCoreSize;
    private Integer poolIdleSize;
    private Integer poolMaxSize;
}
