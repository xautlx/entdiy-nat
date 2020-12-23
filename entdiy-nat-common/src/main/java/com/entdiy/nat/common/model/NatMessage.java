/**
 * Copyright @ 2020-2020 EntDIY NAT (like Ngrok) based on Netty
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
package com.entdiy.nat.common.model;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;

@ToString
public class NatMessage {

    private NatMessage() {

    }

    public static NatMessage build() {
        NatMessage message = new NatMessage();
        message.setCrcCode(CRC_CODE);
        return message;
    }

    public final static int CRC_CODE = 0xabef0101;

    @Setter(AccessLevel.PRIVATE)
    @Getter
    @ToString.Exclude
    private int crcCode;

    /**
     * 消息体长度
     */
    @Getter
    @Setter
    private int length;

    /**
     * 协议类型
     *
     * @see ProtocolType#getCode()
     */
    @Setter
    @Getter
    private byte protocol;

    /**
     * 数据类型
     */
    @Setter
    @Getter
    private byte type;

    /**
     * 消息体
     */
    @Getter
    @ToString.Exclude
    private byte[] body;

    public void setBody(byte[] body) {
        this.body = body;
        this.length = body.length;
    }

    @ToString.Include
    public String getProtocolString() {
        return Arrays.stream(ProtocolType.values()).filter(one -> one.getCode() == this.protocol).findFirst().get().toString();
    }

    @ToString.Include
    public String getTypeString() {
        if (this.protocol == ProtocolType.CONTROL.getCode()) {
            return Arrays.stream(ControlMessageType.values()).filter(one -> one.getCode() == this.type).findFirst().get().toString();
        }else if (this.protocol == ProtocolType.PROXY.getCode()) {
            return Arrays.stream(ProxyMessageType.values()).filter(one -> one.getCode() == this.type).findFirst().get().toString();
        }
        return "N/A";
    }

    @ToString.Include
    public String getBodyString() {
        return new String(this.body);
    }
}
