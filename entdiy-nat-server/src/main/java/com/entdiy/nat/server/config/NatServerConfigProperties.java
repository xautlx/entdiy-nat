package com.entdiy.nat.server.config;

import com.entdiy.nat.common.model.Tunnel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "nat")
public class NatServerConfigProperties {
    private String domain;
    private Integer httpAddr;
    private Integer httpsAddr;
    private Integer tunnelAddr;

    private String version;
    private String mmVersion;

    private List<Tunnel> tunnels;
}
