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

import com.entdiy.nat.common.constant.Constant;
import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatMessageEncoder extends MessageToByteEncoder<NatMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NatMessage msg, ByteBuf out) throws Exception {
        log.trace("Encode message for channel: {}, message: {}", ctx.channel(), msg);
        out.writeInt(msg.getCrcCode());
        out.writeInt(msg.getLength());
        out.writeByte(msg.getProtocol());
        out.writeByte(msg.getType());
        out.writeBytes(msg.getBody());
        out.writeBytes(Constant.DELIMITER_BYTES);
    }
}
