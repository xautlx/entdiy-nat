package com.entdiy.nat.common.constant;


public enum ProxyMessageType {

    StartProxy(Byte.valueOf("7")),
    RemoteProxy(Byte.valueOf("8"));

    private byte code;

    ProxyMessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
