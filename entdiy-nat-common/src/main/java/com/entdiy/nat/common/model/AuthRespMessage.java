package com.entdiy.nat.common.model;

import lombok.Data;

@Data
public class AuthRespMessage extends BaseResponseMessage {
    private String version;
    private String mmVersion;
    private String clientToken;
}
