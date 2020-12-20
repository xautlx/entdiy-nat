package com.entdiy.nat.tester;

import com.entdiy.nat.tester.handler.TesterHandler;
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class NatTesterApplication {

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workGroup = new NioEventLoopGroup();
    private ServerBootstrap b = new ServerBootstrap();

    public static void main(String[] args) {
        SpringApplication.run(NatTesterApplication.class, args);
    }

    @PostConstruct
    public void init() {
        try {
            b.group(bossGroup, workGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new TesterHandler());
                }
            });

            ChannelFuture f = b.bind(8888).sync();
            if (f.isSuccess()) {
                log.info("Listening for control and proxy connections on [::]: {}", f.channel().localAddress());
            }
        } catch (Exception e) {
            log.error("Server error", e);
        }
    }

}
