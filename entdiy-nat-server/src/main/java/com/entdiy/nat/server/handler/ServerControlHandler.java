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

import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.AuthMessage;
import com.entdiy.nat.common.model.AuthRespMessage;
import com.entdiy.nat.common.model.InitProxyMessage;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.NewTunnelMessage;
import com.entdiy.nat.common.model.RegProxyMessage;
import com.entdiy.nat.common.model.ReqTunnelMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.ServerContext;
import com.entdiy.nat.server.codec.NatHttpResponseDecoder;
import com.entdiy.nat.server.config.NatServerConfigProperties;
import com.entdiy.nat.server.service.ControlService;
import com.entdiy.nat.server.support.NatClient;
import com.entdiy.nat.server.support.ProxyChannelSource;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ServerControlHandler extends NatCommonHandler {

    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static Map<String, String> reqIdUrlMapping = new HashMap<>();

    private static Set<Integer> listeningRemotePorts = new HashSet<>();

    private NatHttpResponseDecoder responseDecoder = new NatHttpResponseDecoder();

    public ServerControlHandler() {

    }

    @Override

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }

        if (msg instanceof NatMessage) {
            try {
                ControlService controlService = ServerContext.getControlService();

                NatMessage messageIn = (NatMessage) msg;
                //优先处理Ping消息，高频反复调用，只做少量日志避免干扰主业务日志
                if (messageIn.getType() == ControlMessageType.Ping.getCode()) {
                    log.trace("Read PING message: {}", messageIn);
                    NatMessage respMessage = NatMessage.build();
                    byte[] respBodyContent = ControlMessageType.Pong.name().getBytes();
                    respMessage.setProtocol(messageIn.getProtocol());
                    respMessage.setType(ControlMessageType.Pong.getCode());
                    respMessage.setBody(respBodyContent);
                    ctx.channel().writeAndFlush(respMessage);

                } else {
                    log.debug("Reading message : {}", messageIn);
                    if (messageIn.getProtocol() == ProtocolType.CONTROL.getCode()) {
                        NatServerConfigProperties config = ServerContext.getConfig();
                        if (messageIn.getType() == ControlMessageType.Auth.getCode()) {
                            String reqBodyString = messageIn.getBodyString();
                            AuthMessage reqBody = JsonUtil.deserialize(reqBodyString, AuthMessage.class);

                            log.debug("Processing auth for client: {}", reqBody.getClient());
                            String clientToken = controlService.authClient(reqBody);

                            AuthRespMessage respBody = new AuthRespMessage();
                            if (clientToken == null) {
                                respBody.setError("Auth failure, please check secret");
                            }
                            respBody.setClientToken(clientToken);
                            respBody.setVersion(config.getVersion());
                            respBody.setMmVersion(config.getMmVersion());
                            byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

                            NatMessage respMessage = NatMessage.build();
                            respMessage.setType(ControlMessageType.AuthResp.getCode());
                            respMessage.setProtocol(ProtocolType.CONTROL.getCode());
                            respMessage.setBody(respBodyContent);
                            log.debug("Writing message : {}", respMessage);
                            ctx.channel().writeAndFlush(respMessage);
                        } else if (messageIn.getType() == ControlMessageType.ReqTunnel.getCode()) {
                            String body = messageIn.getBodyString();
                            ReqTunnelMessage bodyMessage = JsonUtil.deserialize(body, ReqTunnelMessage.class);
                            String client = controlService.validateClientToken(bodyMessage.getClientToken());
                            log.debug("ReqTunnel for client: {} with: {}", client, bodyMessage);

                            String url = bodyMessage.getRemotePort() > 0 ?
                                    (bodyMessage.getProtocol() + ":" + bodyMessage.getRemotePort()) :
                                    (bodyMessage.getProtocol() + ":" + bodyMessage.getSubdomain());
                            String reqId = bodyMessage.getReqId();
                            reqIdUrlMapping.put(reqId, url);

                            NewTunnelMessage respBody = new NewTunnelMessage();
                            respBody.setReqId(reqId);
                            respBody.setProtocol(bodyMessage.getProtocol());
                            respBody.setUrl(url);

                            if (bodyMessage.getRemotePort() != null) {
                                if (listeningRemotePorts.contains(bodyMessage.getRemotePort())) {
                                    String error = "RemotePort '" + bodyMessage.getRemotePort() + "' NOT available for server bind";
                                    log.warn(error + " for Tunnel: {}", bodyMessage);
                                    respBody.setError(error);
                                } else {
                                    ServerBootstrap b = new ServerBootstrap();
                                    b.group(bossGroup, workerGroup)
                                            .channel(NioServerSocketChannel.class)
                                            .option(ChannelOption.SO_BACKLOG, 100)
                                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                                @Override
                                                public void initChannel(SocketChannel ch) {
                                                    ChannelPipeline p = ch.pipeline();
                                                    p.addLast(new LoggingHandler());
                                                    p.addLast(new IdleStateHandler(10, 20, 25));
                                                    p.addLast(new NatMessageEncoder());
                                                    if (ProtocolType.HTTP.name().equalsIgnoreCase(bodyMessage.getProtocol())) {
                                                        p.addLast(new HttpRequestDecoder());
                                                        p.addLast(new HttpObjectAggregator(2 * 1024 * 1024));
                                                    }
                                                    p.addLast(new RemotePortHandler(client, url));
                                                }
                                            });

                                    ChannelFuture f = b.bind(bodyMessage.getRemotePort()).sync();
                                    log.info("Listening remote channel: {}", f.channel());
                                    listeningRemotePorts.add(bodyMessage.getRemotePort());
                                }
                            }

                            byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

                            NatMessage respMessage = NatMessage.build();
                            respMessage.setType(ControlMessageType.NewTunnel.getCode());
                            respMessage.setProtocol(ProtocolType.CONTROL.getCode());
                            respMessage.setBody(respBodyContent);
                            log.debug("Writing message : {}", respMessage);
                            ctx.channel().writeAndFlush(respMessage);
                            //TODO HTTP处理
                        } else if (messageIn.getType() == ControlMessageType.InitProxy.getCode()) {
                            String body = messageIn.getBodyString();
                            InitProxyMessage bodyMessage = JsonUtil.deserialize(body, InitProxyMessage.class);
                            String client = controlService.validateClientToken(bodyMessage.getClientToken());

                            log.debug("InitProxy for client: {} with: {}", client, bodyMessage);
                            NatClient natClient = new NatClient();
                            natClient.setClient(client);
                            natClient.setPoolCoreSize(bodyMessage.getCoreSize());
                            natClient.setPoolIdleSize(bodyMessage.getIdleSize());
                            natClient.setPoolMaxSize(bodyMessage.getMaxSize());
                            ProxyChannelSource.init(ctx.channel(), natClient);
                        } else if (messageIn.getType() == ControlMessageType.RegProxy.getCode()) {
                            String body = messageIn.getBodyString();
                            RegProxyMessage bodyMessage = JsonUtil.deserialize(body, RegProxyMessage.class);
                            String client = controlService.validateClientToken(bodyMessage.getClientToken());

                            log.debug("RegProxy client: {}", client);
                            ProxyChannelSource.add(client, ctx.channel());
                        }
                    } else if (messageIn.getProtocol() == ProtocolType.TCP.getCode()) {
                        if (messageIn.getType() == ProxyMessageType.ProxyResp.getCode()) {
                            Channel remoteChannel = RemotePortHandler.getRemoteChannel(ctx.channel());
                            if (remoteChannel != null) {
                                ByteBuf byteBuf = Unpooled.copiedBuffer(messageIn.getBody());
                                log.debug("Write message to remote channel: {}, data length: {}", remoteChannel, byteBuf.readableBytes());
                                remoteChannel.writeAndFlush(byteBuf);
                            }
                        }
                    } else if (messageIn.getProtocol() == ProtocolType.HTTP.getCode()) {
                        if (messageIn.getType() == ProxyMessageType.ProxyResp.getCode()) {
                            Channel remoteChannel = RemotePortHandler.getRemoteChannel(ctx.channel());
                            if (remoteChannel != null) {
                                ByteBuf byteBuf = Unpooled.copiedBuffer(messageIn.getBody());
//                                List<Object> out = Lists.newArrayList();
//                                responseDecoder.decode(ctx,byteBuf,out);
//                                //TODO 301 302等转换处理
//                                HttpResponse httpResponse=(HttpResponse)out.get(0);
//                                //httpResponse.headers()
                                log.debug("Write message to remote channel: {}, data length: {}", remoteChannel, byteBuf.readableBytes());
                                remoteChannel.writeAndFlush(byteBuf);
                            }
                        }
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }
}
