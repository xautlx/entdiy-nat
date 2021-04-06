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
package com.entdiy.nat.client.handler;

import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TargetTcpHandler extends NatCommonHandler {

    private Channel proxyChannel;

    public TargetTcpHandler(Channel proxyChannel) {
        this.proxyChannel = proxyChannel;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel targetChannel = ctx.channel();
        log.info("Disconnect to target channel: {}", targetChannel);
        ClientProxyHandler.removeChannelMapping(targetChannel);
        proxyChannel.close();
        targetChannel.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) {
        try {
            ByteBuf byteBuf = (ByteBuf) msg;
            NatMessage natMessage = NatMessage.build();
            natMessage.setType(ProxyMessageType.ProxyResp.getCode());
            natMessage.setProtocol(ProtocolType.TCP.getCode());
            natMessage.setBody(ByteBufUtil.getBytes(byteBuf));
            log.info("Write TCP message back to proxy channel: {}, {} bytes", proxyChannel, byteBuf.readableBytes());
            proxyChannel.writeAndFlush(natMessage);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
