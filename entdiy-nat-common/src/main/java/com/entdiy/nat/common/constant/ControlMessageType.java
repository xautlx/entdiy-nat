/**
 * Copyright @ 2020-2020 EntDIY-NAT (like Ngrok) based on Netty
 *
 * Author: Li Xia, E-Mail: xautlx@hotmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
