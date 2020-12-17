package com.entdiy.nat.common.constant;


import com.entdiy.nat.common.model.AuthMessage;

public enum ControlMessageType {

    /**
     * @see AuthMessage
     */
    Auth(Byte.valueOf("1")),

    /**
     * type AuthResp struct {
     * Version   string
     * MmVersion string
     * ClientId  string
     * Error     string
     * }
     */
    AuthResp(Byte.valueOf("2")),
    ReqTunnel(Byte.valueOf("3")),
    NewTunnel(Byte.valueOf("4")),
    ReqProxy(Byte.valueOf("5")),
    RegProxy(Byte.valueOf("6")),
    StartProxy(Byte.valueOf("7")),
    Ping(Byte.valueOf("8")),
    Pong(Byte.valueOf("9")),
    NOT_ACCEPTABLE(Byte.valueOf("90"));

    private byte code;

    ControlMessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
