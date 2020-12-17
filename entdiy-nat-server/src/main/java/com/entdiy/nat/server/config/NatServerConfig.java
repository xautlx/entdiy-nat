package com.entdiy.nat.server.config;

import com.entdiy.nat.server.service.ControlService;
import com.entdiy.nat.server.service.SimpleControlService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatServerConfig {

    @Bean
    public ControlService controlService() {
        return new SimpleControlService();
    }
}
