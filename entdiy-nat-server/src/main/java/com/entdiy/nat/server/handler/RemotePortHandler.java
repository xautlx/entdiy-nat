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
import com.entdiy.nat.server.support.ProxyChannelSource;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RemotePortHandler extends NatCommonHandler {


    private String clientToken;
    private String url;

    private static BiMap<String, Channel> clientAddrPublicChannelMapping = HashBiMap.create();
    private static BiMap<String, Channel> clientAddrProxyChannelMapping = HashBiMap.create();

    public RemotePortHandler(String clientToken, String url) {
        this.clientToken = clientToken;
        this.url = url;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel remoteChannel = ctx.channel();
        String clientAddr = remoteChannel.remoteAddress().toString();
        log.debug("RemotePortHandler channelActive: {}", remoteChannel);
        clientAddrPublicChannelMapping.put(clientAddr, remoteChannel);

        Channel proxyChannel = ProxyChannelSource.acquire(clientToken);
        clientAddrProxyChannelMapping.put(clientAddr, proxyChannel);
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
        String clientAddr = clientAddrPublicChannelMapping.inverse().get(remoteChannel);
        Channel proxyChannel = clientAddrProxyChannelMapping.get(clientAddr);
        clientAddrProxyChannelMapping.remove(clientAddr);
        clientAddrPublicChannelMapping.remove(clientAddr);
        proxyChannel.close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel remoteChannel = ctx.channel();
        log.debug("RemotePortHandler channelRead: {}", remoteChannel);
        if (msg == null) {
            return;
        }

        String clientAddr = clientAddrPublicChannelMapping.inverse().get(remoteChannel);
        Channel proxyChannel = clientAddrProxyChannelMapping.get(clientAddr);
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("Write message to proxy channel: {}, data length: {}", proxyChannel, byteBuf.readableBytes());
        proxyChannel.writeAndFlush(msg);
    }

    public static Channel getPublicChannel(Channel proxyChannel) {
        String clientAddr = clientAddrProxyChannelMapping.inverse().get(proxyChannel);
        return clientAddrPublicChannelMapping.get(clientAddr);
    }
}
