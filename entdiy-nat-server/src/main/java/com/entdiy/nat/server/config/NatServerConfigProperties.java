package com.entdiy.nat.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nat")
public class NatServerConfigProperties {
    private String domain;
    private Integer httpAddr;
    private Integer httpsAddr;
    private Integer tunnelAddr;

    private String version;
    private String mmVersion;
}
