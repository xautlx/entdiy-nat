package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.AuthMessage;
import com.entdiy.nat.common.model.AuthRespMessage;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.NewTunnelMessage;
import com.entdiy.nat.common.model.RegProxyMessage;
import com.entdiy.nat.common.model.ReqTunnelMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.ServerContext;
import com.entdiy.nat.server.config.NatServerConfigProperties;
import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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


    private static Map<String, Channel> clientProxyChannelMapping = new HashMap<>();
    private static Map<String, String> reqIdUrlMapping = new HashMap<>();

    private static Set<Integer> listeningRemotePorts = new HashSet<>();

    @Override

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }

        if (msg instanceof NatMessage) {
            try {
                NatMessage messageIn = (NatMessage) msg;
                //优先处理Ping消息，高频反复调用，只做少量日志避免干扰主业务日志
                if (messageIn.getType() == ControlMessageType.Ping.getCode()) {
                    log.trace("Read PING message: {}", messageIn);
                    NatMessage respMessage = NatMessage.build();
                    byte[] respBodyContent = ControlMessageType.Pong.name().getBytes();
                    respMessage.setProtocol(messageIn.getProtocol());
                    respMessage.setType(ControlMessageType.Pong.getCode());
                    respMessage.setBody(respBodyContent);
                    ctx.writeAndFlush(respMessage);

                } else {
                    log.debug("Reading message : {}", messageIn);
                    if (messageIn.getProtocol() == ProtocolType.CONTROL.getCode()) {
                        if (messageIn.getType() == ControlMessageType.Auth.getCode()) {
                            String reqBodyString = messageIn.getBodyString();
                            AuthMessage reqBody = JsonUtil.deserialize(reqBodyString, AuthMessage.class);

                            log.debug("Processing Auth for: {}", reqBody.getClientId());
                            NatServerConfigProperties config = ServerContext.getConfig();
                            String clientToken = ServerContext.getControlService().authClient(reqBody);

                            AuthRespMessage respBody = new AuthRespMessage();
                            respBody.setClientToken(clientToken);
                            respBody.setVersion(config.getVersion());
                            respBody.setMmVersion(config.getMmVersion());
                            byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

                            NatMessage respMessage = NatMessage.build();
                            respMessage.setType(ControlMessageType.AuthResp.getCode());
                            respMessage.setProtocol(ProtocolType.CONTROL.getCode());
                            respMessage.setBody(respBodyContent);
                            log.debug("Writing message : {}", respMessage);
                            ctx.writeAndFlush(respMessage);

                            //As a performance optimization, ask for a proxy connection up front
//                            NatMessage reqProxyMessage = NatMessage.build();
//                            reqProxyMessage.setType(ControlMessageType.ReqProxy.getCode());
//                            reqProxyMessage.setProtocol(ProtocolType.CONTROL.getCode());
//                            reqProxyMessage.setBody(JsonUtil.serialize(new ReqProxyMessage()).getBytes());
//                            log.debug("Writing message : {}", reqProxyMessage);
//                            ctx.writeAndFlush(reqProxyMessage);
                        } else if (messageIn.getType() == ControlMessageType.ReqTunnel.getCode()) {
                            String body = messageIn.getBodyString();
                            ReqTunnelMessage bodyMessage = JsonUtil.deserialize(body, ReqTunnelMessage.class);

                            String url = bodyMessage.getRemotePort() > 0 ?
                                    (bodyMessage.getProtocol() + ":" + bodyMessage.getRemotePort()) :
                                    (bodyMessage.getProtocol() + ":" + bodyMessage.getSubdomain());
                            String reqId = bodyMessage.getReqId();
                            reqIdUrlMapping.put(reqId, url);

                            NewTunnelMessage respBody = new NewTunnelMessage();
                            respBody.setReqId(reqId);
                            respBody.setProtocol(bodyMessage.getProtocol());
                            respBody.setUrl(url);
                            byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

                            NatMessage respMessage = NatMessage.build();
                            respMessage.setType(ControlMessageType.NewTunnel.getCode());
                            respMessage.setProtocol(ProtocolType.CONTROL.getCode());
                            respMessage.setBody(respBodyContent);
                            log.debug("Writing message : {}", respMessage);
                            ctx.writeAndFlush(respMessage);

                            if (bodyMessage.getRemotePort() != null
                                    && bodyMessage.getRemotePort() > -1
                                    && !listeningRemotePorts.contains(bodyMessage.getRemotePort())) {
                                ServerBootstrap b = new ServerBootstrap();
                                b.group(bossGroup, workerGroup)
                                        .channel(NioServerSocketChannel.class)
                                        .option(ChannelOption.SO_BACKLOG, 100)
                                        .handler(new LoggingHandler(LogLevel.INFO))
                                        .childHandler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            public void initChannel(SocketChannel ch) {
                                                ChannelPipeline p = ch.pipeline();
                                                p.addLast(new NatMessageEncoder());
                                                p.addLast(new RemotePortHandler(bodyMessage.getClientToken(), url));
                                            }
                                        });

                                ChannelFuture f = b.bind(bodyMessage.getRemotePort()).sync();
                                log.info("Listening remote port: {}", bodyMessage.getRemotePort());
                                listeningRemotePorts.add(bodyMessage.getRemotePort());
                                f.channel().closeFuture().sync();
                            }
                            //TODO HTTP处理
                        } else if (messageIn.getType() == ControlMessageType.RegProxy.getCode()) {
                            String body = messageIn.getBodyString();
                            RegProxyMessage bodyMessage = JsonUtil.deserialize(body, RegProxyMessage.class);
                            log.info("RegProxy client: {}", bodyMessage.getClientToken());
                            clientProxyChannelMapping.put(bodyMessage.getClientToken(), ctx.channel());
                        } else if (messageIn.getType() == ControlMessageType.RegProxy.getCode()) {
                            String body = messageIn.getBodyString();
                            RegProxyMessage bodyMessage = JsonUtil.deserialize(body, RegProxyMessage.class);
                            log.info("RegProxy client: {}", bodyMessage.getClientToken());
                            clientProxyChannelMapping.put(bodyMessage.getClientToken(), ctx.channel());
                        }
                    }
                }else if (messageIn.getProtocol() == ProtocolType.PROXY.getCode()) {

                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            log.info("Proxy data ...");
        }
    }

    public static Channel getProxyChannelByClientToken(String clientToken) {
        return clientProxyChannelMapping.get(clientToken);
    }
}
