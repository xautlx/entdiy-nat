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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

@Slf4j
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

    private Tunnel tunnel;
    private String clientToken;
    private boolean init = false;
    private ChannelFuture f;
    private static NioEventLoopGroup group = new NioEventLoopGroup();

    private BiMap<String, Channel> clientAddrChannelMapping = HashBiMap.create();

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
                        String reqBodyString = messageIn.getBodyString();
                        StartProxyMessage reqBody = JsonUtil.deserialize(reqBodyString, StartProxyMessage.class);

                        NatClientConfigProperties config = ClientContext.getConfig();
                        Channel targetChannel = clientAddrChannelMapping.get(reqBody.getClientAddr());
                        if (targetChannel == null) {
                            try {
                                Bootstrap b = new Bootstrap();
                                b.group(group)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.TCP_NODELAY, true)
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) throws SSLException {
                                                ChannelPipeline p = ch.pipeline();
                                                p.addLast(new ClientProxyHandler(clientToken));
                                            }
                                        });
                                ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
                                log.info("Connect to remote address {} for {}", f.channel().remoteAddress(), ControlMessageType.ReqProxy.name());
                                f.channel().closeFuture().sync();
                            } catch (InterruptedException e) {
                                log.error("Proxy connect error", e);
                            }

                            try {
                                Bootstrap b = new Bootstrap();
                                b.group(group)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.TCP_NODELAY, true)
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) {
                                                ChannelPipeline p = ch.pipeline();
                                                p.addLast(new FetchDataHandler(reqBody.getClientAddr(), ctx.channel()));
                                            }
                                        });
                                Tunnel tunnel = ClientTunnelHandler.getByUrl(reqBody.getUrl());
                                ChannelFuture f = b.connect(tunnel.getHost(), tunnel.getPort()).sync();
                                Channel channel = f.channel();
                                clientAddrChannelMapping.put(reqBody.getClientAddr(), channel);
                                log.info("Connect to local service: {} ", channel.remoteAddress());
                                channel.closeFuture().addListener((ChannelFutureListener) t -> {
                                    Channel c = t.channel();
                                    log.info("Disconnect to local service: {}", c.localAddress());
                                    clientAddrChannelMapping.remove(clientAddrChannelMapping.inverse().get(c));
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
            ByteBuf byteBuf = (ByteBuf) msg;
            log.info("Proxy write message to local port: {}, data length: {}", f.channel().localAddress(), byteBuf.readableBytes());
            f.channel().writeAndFlush(byteBuf.copy());
        }
    }
}
