package com.entdiy.nat.client.handler;

import com.entdiy.nat.client.ClientContext;
import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.AuthMessage;
import com.entdiy.nat.common.model.AuthRespMessage;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.NewTunnelMessage;
import com.entdiy.nat.common.model.ReqTunnelMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
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
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ClientControlHandler extends NatCommonHandler {

    private static NioEventLoopGroup group = new NioEventLoopGroup(1);

    private String clientToken;

    private static Map<String, Tunnel> reqIdTunnelMapping = new HashMap<>();
    private static Map<String, Tunnel> urlTunnelMapping = new HashMap<>();

    private boolean reconnectFlag;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Tunnel client channelInactive");
        if (reconnectFlag) {
            //     NatClientListener.reconnect();
        } else {
            System.exit(0);
        }
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

                            List<Tunnel> tunnels = config.getTunnels();
                            for (Tunnel tunnel : tunnels) {
                                Assert.hasText(tunnel.getName(), "Tunnel 'name' required");
                                ReqTunnelMessage bodyMessage = new ReqTunnelMessage();
                                bodyMessage.setClientToken(clientToken);
                                String reqId = UUID.randomUUID().toString();
                                reqIdTunnelMapping.put(reqId, tunnel);
                                bodyMessage.setReqId(reqId);
                                bodyMessage.setHostname(tunnel.getHost());
                                if (tunnel.getRemotePort() != null) {
                                    bodyMessage.setRemotePort(tunnel.getRemotePort());
                                    bodyMessage.setProtocol(ProtocolType.TCP.name());
                                }
                                //TODO HTTP处理
                                byte[] bodyContent = JsonUtil.serialize(bodyMessage).getBytes();

                                NatMessage message = NatMessage.build();
                                message.setProtocol(ProtocolType.CONTROL.getCode());
                                message.setType(ControlMessageType.ReqTunnel.getCode());
                                message.setBody(bodyContent);
                                log.trace("Write message: {}", message);
                                ctx.channel().writeAndFlush(message);
                            }

                        } else if (messageIn.getType() == ControlMessageType.ReqProxy.getCode()) {
                            Bootstrap b = new Bootstrap();
                            try {
                                b.group(group)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.TCP_NODELAY, true)
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) throws SSLException {
                                                SSLEngine engine = SslContextBuilder.forClient()
                                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                        .build()
                                                        .newEngine(ch.alloc());
                                                ChannelPipeline p = ch.pipeline();
                                                p.addFirst(new SslHandler(engine, false));
                                                p.addLast(new NatMessageDecoder());
                                                p.addLast(new NatMessageEncoder());
                                                p.addLast(new ClientProxyHandler(clientToken));
                                            }
                                        });
                                ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
                                log.info("Connect to remote address " + f.channel().remoteAddress());
                                f.channel().closeFuture().addListener(
                                        (ChannelFutureListener) channelFuture -> log.info("Disconnect to remote address " + f.channel().remoteAddress())
                                );
                            } catch (InterruptedException e) {
                                log.error("Proxy connect error", e);
                            }
                        } else if (messageIn.getType() == ControlMessageType.NewTunnel.getCode()) {
                            String reqBodyString = messageIn.getBodyString();
                            NewTunnelMessage reqBody = JsonUtil.deserialize(reqBodyString, NewTunnelMessage.class);
                            urlTunnelMapping.put(reqBody.getUrl(), reqIdTunnelMapping.get(reqBody.getReqId()));
                            log.info("New tunnel created: {}", messageIn.getBodyString());
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

    public static Tunnel getByUrl(String url) {
        return urlTunnelMapping.get(url);
    }
}
