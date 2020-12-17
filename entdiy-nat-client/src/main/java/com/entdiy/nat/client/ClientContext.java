package com.entdiy.nat.client;

import com.entdiy.nat.client.config.NatClientConfigProperties;

public class ClientContext {

    private static NatClientConfigProperties config;

    public static void setConfig(NatClientConfigProperties config) {
        ClientContext.config = config;
    }

    public static NatClientConfigProperties getConfig() {
        return config;
    }

}
