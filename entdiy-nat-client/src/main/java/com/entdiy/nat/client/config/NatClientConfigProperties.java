package com.entdiy.nat.client.config;

import com.entdiy.nat.common.model.Tunnel;
import io.netty.handler.logging.LogLevel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "nat")
public class NatClientConfigProperties {
    private Boolean idlePingEnabled = Boolean.TRUE;
    private String clientId;
    private String clientSecret;
    private String version;
    private String mmVersion;

    private int reconnectSeconds = 10;
    private String serverAddr;
    private int port = -1;
    private String domain;

    private Integer poolCoreSize;
    private Integer poolIdleSize;
    private Integer poolMaxSize;

    private List<Tunnel> tunnels;

    // TODO 待扩展为从参数读取
    public LogLevel getHandlerLogLevel() {
        return LogLevel.INFO;
    }
}
