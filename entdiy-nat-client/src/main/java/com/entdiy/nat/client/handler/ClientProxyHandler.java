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

import com.entdiy.nat.client.ClientContext;
import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.RegProxyMessage;
import com.entdiy.nat.common.model.StartProxyMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

    private String clientToken;
    private static NioEventLoopGroup group = new NioEventLoopGroup();

    private BiMap<Channel, Channel> targetProxyChannelMapping = HashBiMap.create();
    private BiMap<String, Channel> urlChannelMapping = HashBiMap.create();

    public ClientProxyHandler(String clientToken) {
        this.clientToken = clientToken;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        RegProxyMessage bodyMessage = new RegProxyMessage();
        bodyMessage.setClientToken(clientToken);
        byte[] bodyContent = JsonUtil.serialize(bodyMessage).getBytes();

        NatMessage message = NatMessage.build();
        message.setProtocol(ProtocolType.CONTROL.getCode());
        message.setType(ControlMessageType.RegProxy.getCode());
        message.setBody(bodyContent);
        log.debug("Write message: {}", message);
        ctx.channel().writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        log.trace("Control read message: {}", msg);
        if (msg == null) {
            return;
        }

        if (msg instanceof NatMessage) {
            try {
                NatMessage messageIn = (NatMessage) msg;
                log.debug("Read message: {}", messageIn);
                if (messageIn.getProtocol() == ProtocolType.PROXY.getCode()) {
                    if (messageIn.getType() == ProxyMessageType.StartProxy.getCode()) {
                        NatClientConfigProperties config = ClientContext.getConfig();
                        String reqBodyString = messageIn.getBodyString();
                        StartProxyMessage reqBody = JsonUtil.deserialize(reqBodyString, StartProxyMessage.class);

                        Channel proxyChannel = ctx.channel();
                        Channel targetChannel = targetProxyChannelMapping.get(proxyChannel);
                        if (targetChannel == null) {
                            try {
                                Bootstrap b = new Bootstrap();
                                b.group(group)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.TCP_NODELAY, true)
                                        .option(ChannelOption.SO_KEEPALIVE, true)
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) {
                                                ChannelPipeline p = ch.pipeline();
                                                p.addLast(new LoggingHandler());
                                                p.addLast(new IdleStateHandler(60, 80, 120));
                                                p.addLast(new ClientLocalHandler(proxyChannel));
                                            }
                                        });
                                Tunnel tunnel = ClientTunnelHandler.getByUrl(reqBody.getUrl());
                                ChannelFuture f = b.connect(tunnel.getHost(), tunnel.getPort()).sync();
                                if (f.isSuccess()) {
                                    targetChannel = f.channel();
                                    targetProxyChannelMapping.put(proxyChannel, targetChannel);
                                    log.info("Connect to local channel: {} ", targetChannel);
//                                    targetChannel.closeFuture().addListener((ChannelFutureListener) t -> {
//                                        Channel closeTargetChannel = t.channel();
//                                        log.info("Disconnect to local channel: {}", closeTargetChannel);
//                                        Channel closeProxyChannel = targetProxyChannelMapping.inverse().get(closeTargetChannel);
//                                        targetProxyChannelMapping.remove(closeProxyChannel);
//                                        closeProxyChannel.close();
//                                    });
                                } else {
                                    proxyChannel.close();
                                }
                            } catch (Exception e) {
                                log.error("Proxy connect error", e);
                                proxyChannel.close();
                            }
                        }
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ByteBuf byteBuf = (ByteBuf) msg;
            Channel targetChannel = targetProxyChannelMapping.get(ctx.channel());
            log.info("Write message to local channel: {}, data length: {}", targetChannel, byteBuf.readableBytes());
            targetChannel.writeAndFlush(byteBuf.copy());
        }
    }
}
