package com.entdiy.nat.server.service;

import com.entdiy.nat.common.model.AuthMessage;
import org.springframework.stereotype.Service;

@Service
public interface ControlService {
    String authClient(AuthMessage authMessage);

    String validateClientToken(String clientToken);
}
