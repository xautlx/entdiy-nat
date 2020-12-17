package com.entdiy.nat.server.listener;

import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.listener.NatCommonListener;
import com.entdiy.nat.server.ServerContext;
import com.entdiy.nat.server.config.NatServerConfigProperties;
import com.entdiy.nat.server.handler.ServerControlHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatServerListener extends NatCommonListener {

    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static final ServerBootstrap b = new ServerBootstrap();

    public NatServerListener() {
        b.group(bossGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new IdleStateHandler(10, 10, 20));
                p.addLast(new NatMessageDecoder());
                p.addLast(new NatMessageEncoder());
                p.addLast(new ServerControlHandler());
            }
        });
    }

    public void run() {
        new Thread(() -> {
            try {
                NatServerConfigProperties config = ServerContext.getConfig();
                ChannelFuture f = b.bind(config.getTunnelAddr()).sync();
                log.info("Listening for control and proxy connections on [::]: {}", config.getTunnelAddr());
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("ServerBootstrap bind error", e);
            } finally {
                bossGroup.shutdownGracefully();
            }
        }).start();

    }
}
