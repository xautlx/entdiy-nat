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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatServerListener extends NatCommonListener {

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workGroup = new NioEventLoopGroup();
    private ServerBootstrap b = new ServerBootstrap();

    public void run() {
        try {
            b.group(bossGroup, workGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new LoggingHandler(LogLevel.DEBUG));
                    p.addLast(new NatMessageDecoder());
                    p.addLast(new NatMessageEncoder());
                    p.addLast(new ServerControlHandler());
                }
            });

            NatServerConfigProperties config = ServerContext.getConfig();
            ChannelFuture f = b.bind(config.getTunnelAddr()).sync();
            if (f.isSuccess()) {
                log.info("Listening for control and proxy connections: {}", f.channel());
            }
        } catch (Exception e) {
            log.error("ServerBootstrap bind error", e);
        } finally {
            //bossGroup.shutdownGracefully();
            //workGroup.shutdownGracefully();
        }
    }
}
