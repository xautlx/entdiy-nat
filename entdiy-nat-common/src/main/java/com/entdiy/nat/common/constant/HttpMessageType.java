package com.entdiy.nat.common.constant;


public enum HttpMessageType {

    REGISTER(Byte.valueOf("1")),
    REGISTER_RESULT(Byte.valueOf("2")),
    CONNECTED(Byte.valueOf("3")),
    DISCONNECTED(Byte.valueOf("4")),
    DATA(Byte.valueOf("5")),
    KEEPALIVE(Byte.valueOf("6"));

    private byte code;

    HttpMessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
