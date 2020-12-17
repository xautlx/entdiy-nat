package com.entdiy.nat.common.constant;


public enum ProtocolType {

    CONTROL(Byte.valueOf("10")),
    PROXY(Byte.valueOf("20")),
    TCP(Byte.valueOf("30")),
    HTTP(Byte.valueOf("40")),
    HTTPS(Byte.valueOf("50"));

    private byte code;

    ProtocolType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
