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
import com.entdiy.nat.client.constant.TunnelsModeEnum;
import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.AuthMessage;
import com.entdiy.nat.common.model.AuthRespMessage;
import com.entdiy.nat.common.model.InitProxyMessage;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.Map;

@Slf4j
public class ClientControlHandler extends NatCommonHandler {

    private static NioEventLoopGroup group = new NioEventLoopGroup();

    private String clientToken;

    private final static Integer MIN_POOL_SIZE = 10;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("ClientControlHandler channelActive: {}", ctx.channel());
        NatClientConfigProperties config = ClientContext.getConfig();
        AuthMessage bodyMessage = new AuthMessage();
        bodyMessage.setClientId(config.getClientId());
        bodyMessage.setClientSecret(config.getClientSecret());
        bodyMessage.setVersion(config.getVersion());
        bodyMessage.setMmVersion(config.getMmVersion());
        byte[] bodyContent = JsonUtil.serialize(bodyMessage).getBytes();

        NatMessage message = NatMessage.build();
        message.setProtocol(ProtocolType.CONTROL.getCode());
        message.setType(ControlMessageType.Auth.getCode());
        message.setBody(bodyContent);
        log.debug("Write message: {}", message);
        ctx.channel().writeAndFlush(message);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) {
        NatClientConfigProperties config = ClientContext.getConfig();
        if (Boolean.FALSE.equals(config.getIdlePingEnabled())) {
            return;
        }
        if (obj instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) obj;
            if (IdleState.ALL_IDLE.equals(event.state())) {
                NatMessage message = NatMessage.build();
                byte[] content = String.valueOf(System.currentTimeMillis()).getBytes();
                message.setProtocol(ProtocolType.CONTROL.getCode());
                message.setType(ControlMessageType.Ping.getCode());
                message.setBody(content);
                log.trace("Write message: {}", message);
                ctx.channel().writeAndFlush(message);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("Control read message: {}", msg);
        if (msg == null) {
            return;
        }

        if (msg instanceof NatMessage) {
            try {
                NatMessage messageIn = (NatMessage) msg;
                //优先处理Ping消息，高频反复调用，只做少量日志避免干扰主业务日志
                if (messageIn.getType() == ControlMessageType.Pong.getCode()) {
                    log.trace("Read message: {}", messageIn);

                } else {
                    log.debug("Read message: {}", messageIn);
                    if (messageIn.getProtocol() == ProtocolType.CONTROL.getCode()) {
                        NatClientConfigProperties config = ClientContext.getConfig();
                        if (messageIn.getType() == ControlMessageType.AuthResp.getCode()) {
                            String reqBodyString = messageIn.getBodyString();
                            AuthRespMessage reqBody = JsonUtil.deserialize(reqBodyString, AuthRespMessage.class);
                            if (StringUtils.hasText(reqBody.getError())) {
                                log.error("Response Error: {}", reqBody.getError());
                                return;
                            }
                            log.debug("Get Auth ClientToken: {}", reqBody.getClientToken());
                            clientToken = reqBody.getClientToken();

                            List<Tunnel> tunnels = Lists.newArrayList();
                            Map<String, Tunnel> configTunnels = config.getTunnels();
                            if (TunnelsModeEnum.server.equals(config.getTunnelsMode())) {
                                configTunnels = reqBody.getTunnels();
                            }
                            if (configTunnels != null && configTunnels.size() > 0) {
                                for (Map.Entry<String, Tunnel> me : configTunnels.entrySet()) {
                                    Tunnel tunnel = me.getValue();
                                    tunnel.setCode(me.getKey());
                                    tunnels.add(tunnel);
                                }
                            }
                            Assert.isTrue(tunnels.size() > 0, "Tunnels can't be empty");

                            for (Tunnel tunnel : tunnels) {
                                try {
                                    Bootstrap b = new Bootstrap();
                                    b.group(group)
                                            .channel(NioSocketChannel.class)
                                            .option(ChannelOption.TCP_NODELAY, true)
                                            .handler(new ChannelInitializer<SocketChannel>() {
                                                @Override
                                                protected void initChannel(SocketChannel ch) throws SSLException {
                                                    ChannelPipeline p = ch.pipeline();
                                                    p.addLast(new LoggingHandler());
                                                    p.addLast(new IdleStateHandler(60, 80, 120));
                                                    p.addLast(new NatMessageDecoder());
                                                    p.addLast(new NatMessageEncoder());
                                                    p.addLast(new ClientTunnelHandler(clientToken, tunnel));
                                                }
                                            });
                                    ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
                                    log.info("Connect to tunnel channel: {}", f.channel());
                                    f.channel().closeFuture().addListener((ChannelFutureListener) t -> {
                                        log.info("Disconnect to tunnel channel: {}", t.channel());
                                    });
                                } catch (InterruptedException e) {
                                    log.error("Proxy connect error", e);
                                }
                            }

                            InitProxyMessage initProxyMessage = new InitProxyMessage();
                            initProxyMessage.setClientToken(clientToken);
                            initProxyMessage.setCoreSize(config.getPoolCoreSize() != null && config.getPoolCoreSize() > MIN_POOL_SIZE ? config.getPoolCoreSize() : MIN_POOL_SIZE);
                            initProxyMessage.setIdleSize(config.getPoolIdleSize() != null && config.getPoolIdleSize() > MIN_POOL_SIZE ? config.getPoolIdleSize() : MIN_POOL_SIZE);
                            initProxyMessage.setMaxSize(config.getPoolMaxSize() != null && config.getPoolMaxSize() > 0 ? config.getPoolMaxSize() : 1000);
                            NatMessage message = NatMessage.build();
                            byte[] content = JsonUtil.serialize(initProxyMessage).getBytes();
                            message.setProtocol(ProtocolType.CONTROL.getCode());
                            message.setType(ControlMessageType.InitProxy.getCode());
                            message.setBody(content);
                            log.debug("Write message: {}", message);
                            ctx.channel().writeAndFlush(message);
                        } else if (messageIn.getType() == ControlMessageType.ReqProxy.getCode()) {
                            try {
                                Bootstrap b = new Bootstrap();
                                b.group(group)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.TCP_NODELAY, true)
                                        .option(ChannelOption.SO_KEEPALIVE, true)
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) throws SSLException {
                                                ChannelPipeline p = ch.pipeline();
                                                p.addLast(new LoggingHandler());
                                                p.addLast(new NatMessageDecoder());
                                                p.addLast(new NatMessageEncoder());
                                                p.addLast(new ClientProxyHandler(clientToken));
                                            }
                                        });
                                ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
                                log.info("Connect to proxy channel: {}", f.channel());
                                f.channel().closeFuture().addListener((ChannelFutureListener) t -> {
                                    log.info("Disconnect to proxy channel: {}", t.channel());
                                });
                            } catch (InterruptedException e) {
                                log.error("Proxy connect error", e);
                            }
                        }
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            throw new UnsupportedOperationException("Invalid message type");
        }
    }


}
