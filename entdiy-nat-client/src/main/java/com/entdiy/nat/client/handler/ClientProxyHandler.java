package com.entdiy.nat.client.handler;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.RegProxyMessage;
import com.entdiy.nat.common.model.StartProxyMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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

@Slf4j
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

    private Tunnel tunnel;
    private String clientToken;
    private boolean init = false;
    private ChannelFuture f;
    private static NioEventLoopGroup group = new NioEventLoopGroup(1);

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
        log.trace("Write message: {}", message);
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
                    if (messageIn.getType() == ControlMessageType.StartProxy.getCode()) {
                        String reqBodyString = messageIn.getBodyString();
                        StartProxyMessage reqBody = JsonUtil.deserialize(reqBodyString, StartProxyMessage.class);

                        Bootstrap b = new Bootstrap();
                        b.group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.TCP_NODELAY, true)
                                .handler(new ChannelInitializer<SocketChannel>() {
                                    @Override
                                    protected void initChannel(SocketChannel ch) {
                                        ChannelPipeline p = ch.pipeline();
                                        p.addLast(new FetchDataHandler(ctx.channel()));
                                    }
                                });
                        Tunnel tunnel = ClientControlHandler.getByUrl(reqBody.getUrl());
                        f = b.connect(tunnel.getHost(), tunnel.getPort()).sync();
                        log.info("Connect to local {}:{} ", tunnel.getHost(), tunnel.getPort());
                        f.channel().closeFuture().addListener((ChannelFutureListener) t -> {
                            log.info("Disconnect to local {}:{} ", tunnel.getHost(), tunnel.getPort());
                            init = false;
                        });
                        init = true;
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            log.info("Proxy write message to local port " + f.channel().localAddress());
            ByteBuf byteBuf = (ByteBuf) msg;
            f.channel().writeAndFlush(byteBuf.copy());
        }
    }
}
