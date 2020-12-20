package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class InitProxyMessage extends BaseRequestMessage {
    private Integer coreSize;
    private Integer idleSize;
    private Integer maxSize;
}
