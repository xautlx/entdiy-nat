package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class AuthMessage {
    private String version;
    private String mmVersion;

    private String clientId;
    private String clientSecret;

    private String os;
    private String arch;
    private String clientToken;
}
