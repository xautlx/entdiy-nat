package com.entdiy.nat.server.service;

import com.entdiy.nat.common.model.AuthMessage;
import org.apache.tomcat.util.codec.binary.Base64;

public class SimpleControlService implements ControlService {


    @Override
    public String authClient(AuthMessage authMessage) {
        //注意安全风险：忽略账号密码验证，直接返回可用clientId值
        return Base64.encodeBase64String(authMessage.getClientId().getBytes());
    }

    @Override
    public String validateClientToken(String clientToken) {
        return new String(Base64.decodeBase64(clientToken));
    }
}
