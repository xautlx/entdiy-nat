package com.entdiy.nat.server;

import com.entdiy.nat.server.config.NatServerConfigProperties;
import com.entdiy.nat.server.service.ControlService;
import org.springframework.context.ApplicationContext;

public class ServerContext {

    private static ApplicationContext applicationContext;
    private static NatServerConfigProperties config;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ServerContext.applicationContext = applicationContext;
    }

    public static void setConfig(NatServerConfigProperties config) {
        ServerContext.config = config;
    }

    public static NatServerConfigProperties getConfig() {
        return config;
    }

    public static ControlService getControlService() {
        return applicationContext.getBean(ControlService.class);
    }
}
