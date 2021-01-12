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
package com.entdiy.nat.common.codec;

import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NatMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) {
        int readableBytes = in.readableBytes();
        log.trace("Decode message for channel: {}, bytes size: {}", ctx.channel(), readableBytes);
        int crcCode = readableBytes > 4 ? in.readInt() : -1;
        if (crcCode == NatMessage.CRC_CODE) {
            NatMessage message = NatMessage.build();
            int length = in.readInt();
            message.setLength(length);
            byte protocol = in.readByte();
            message.setProtocol(protocol);
            byte type = in.readByte();
            message.setType(type);
            byte[] body = new byte[length];
            in.readBytes(body);
            message.setBody(body);
            out.add(message);
            log.trace("Decode to message: {}", message);
        } else {
            in.resetReaderIndex();
            in.retain();
            out.add(in);
            log.trace("Decode retain message directly");
        }
    }
}
