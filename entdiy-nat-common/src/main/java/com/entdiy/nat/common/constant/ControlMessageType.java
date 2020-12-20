package com.entdiy.nat.common.constant;


import com.entdiy.nat.common.model.AuthMessage;

public enum ControlMessageType {

    /**
     * @see AuthMessage
     */
    Auth(Byte.valueOf("5")),

    /**
     * type AuthResp struct {
     * Version   string
     * MmVersion string
     * ClientId  string
     * Error     string
     * }
     */
    AuthResp(Byte.valueOf("10")),
    ReqTunnel(Byte.valueOf("15")),
    NewTunnel(Byte.valueOf("20")),
    InitProxy(Byte.valueOf("25")),
    ReqProxy(Byte.valueOf("30")),
    RegProxy(Byte.valueOf("35")),
    Ping(Byte.valueOf("40")),
    Pong(Byte.valueOf("45")),
    NOT_ACCEPTABLE(Byte.valueOf("90"));

    private byte code;

    ControlMessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
