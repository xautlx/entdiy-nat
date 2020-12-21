package com.entdiy.nat.client.listener;

import com.entdiy.nat.client.ClientContext;
import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.client.handler.ClientControlHandler;
import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatClientListener {

    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static Bootstrap b = new Bootstrap();

    public NatClientListener() {
        try {
            SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //    p.addLast(sslCtx.newHandler(ch.alloc()));
                            p.addLast(new IdleStateHandler(60, 80, 120));
                            p.addLast(new LoggingHandler(LogLevel.DEBUG));
                            p.addLast(new NatMessageDecoder());
                            p.addLast(new NatMessageEncoder());
                            p.addLast(new ClientControlHandler());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            NatClientConfigProperties config = ClientContext.getConfig();
            log.debug("Start connect...");
            b.connect(config.getServerAddr(), config.getPort()).sync();
        } catch (Exception e) {
            log.error("ERROR", e);
        }
    }
}
