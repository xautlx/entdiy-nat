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
package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.StartProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.codec.NatHttpRequestEncoder;
import com.entdiy.nat.server.support.ProxyChannelSource;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class RemotePortHandler extends NatCommonHandler {


    private String client;
    private String url;

    private static Map<Channel, Channel> remoteChannelToProxyChannelMapping = new HashMap<>();

    private NatHttpRequestEncoder requestEncoder = new NatHttpRequestEncoder();

    public RemotePortHandler(String client, String url) {
        this.client = client;
        this.url = url;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        if(cause instanceof IOException) {
            log.debug("Remote Channel {}, client: {}, url: {}", channel, client, url, cause);
        }else{
            log.warn("Remote Channel {}, client: {}, url: {}", channel, client, url, cause);
        }
        //异常不关闭连接，以便持续接收外部请求
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel remoteChannel = ctx.channel();
        String clientAddr = remoteChannel.remoteAddress().toString();
        log.debug("RemotePortHandler channelActive: {}", remoteChannel);
        Channel proxyChannel = ProxyChannelSource.acquire(client);
        if (proxyChannel == null) {
            log.warn("Acquire proxy channel failure.");
            return;
        }
        remoteChannelToProxyChannelMapping.put(remoteChannel, proxyChannel);

        StartProxyMessage respBody = new StartProxyMessage();
        respBody.setUrl(url);
        respBody.setClientAddr(clientAddr);
        byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

        NatMessage respMessage = NatMessage.build();
        respMessage.setType(ProxyMessageType.StartProxy.getCode());
        respMessage.setProtocol(ProtocolType.PROXY.getCode());
        respMessage.setBody(respBodyContent);
        proxyChannel.writeAndFlush(respMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel remoteChannel = ctx.channel();
        log.debug("RemotePortHandler channelInactive: {}", remoteChannel);
        Channel proxyChannel = remoteChannelToProxyChannelMapping.get(remoteChannel);
        if (proxyChannel != null) {
            remoteChannelToProxyChannelMapping.remove(remoteChannel);
            log.debug("Closing proxy channel: {}", proxyChannel);
            proxyChannel.close();
        }
        remoteChannel.close();

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel remoteChannel = ctx.channel();
        log.debug("RemotePortHandler channelRead: {}", remoteChannel);
        if (msg == null) {
            return;
        }

        try {
            Channel proxyChannel = remoteChannelToProxyChannelMapping.get(remoteChannel);
            if (proxyChannel == null) {
                remoteChannel.close();
                return;
            }

            NatMessage natMessage = NatMessage.build();
            natMessage.setType(ProxyMessageType.Proxy.getCode());
            if (msg instanceof FullHttpRequest) {
                List<Object> out = new ArrayList<>();
                requestEncoder.encode(ctx, msg, out);
                ByteBuf outByteBuf = (ByteBuf) out.get(0);
                natMessage.setProtocol(ProtocolType.HTTP.getCode());
                natMessage.setBody(ByteBufUtil.getBytes(outByteBuf));
                log.debug("Write HTTP/port message to proxy channel: {}, data size: {}", proxyChannel, outByteBuf.readableBytes());
                proxyChannel.writeAndFlush(natMessage);
            } else if (msg instanceof ByteBuf) {
                ByteBuf outByteBuf = (ByteBuf) msg;
                natMessage.setProtocol(ProtocolType.TCP.getCode());
                natMessage.setBody(ByteBufUtil.getBytes(outByteBuf));
                log.debug("Write TCP message to proxy channel: {}, data size: {}", proxyChannel, outByteBuf.readableBytes());
                proxyChannel.writeAndFlush(natMessage);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    public static Channel getRemoteChannel(Channel proxyChannel) {
        for (Map.Entry<Channel, Channel> me : remoteChannelToProxyChannelMapping.entrySet()) {
            if (me.getValue().equals(proxyChannel)) {
                return me.getKey();
            }
        }
        return null;
    }

    public static Channel removeChannelMapping(Channel proxyChannel) {
        Channel remoteChannel = getRemoteChannel(proxyChannel);
        if (remoteChannel != null) {
            remoteChannelToProxyChannelMapping.remove(remoteChannel);
        }
        return remoteChannel;
    }
}
