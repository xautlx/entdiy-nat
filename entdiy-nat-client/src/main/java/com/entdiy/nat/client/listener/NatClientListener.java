package com.entdiy.nat.client.listener;

import com.entdiy.nat.client.ClientContext;
import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.client.handler.ClientControlHandler;
import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NatClientListener {

    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static Bootstrap b = new Bootstrap();

    public NatClientListener() {
        try {
            SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //    p.addLast(sslCtx.newHandler(ch.alloc()));
                            p.addLast(new IdleStateHandler(5, 5, 10));
                            p.addLast(new NatMessageDecoder());
                            p.addLast(new NatMessageEncoder());
                            p.addLast(new ClientControlHandler());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public ChannelFuture connect() throws Exception {
        log.debug("Start connect...");
        NatClientConfigProperties config = ClientContext.getConfig();
        ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
        f.addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {

                //TODO 服务端配置合并

//                List<Tunnel> tunnels = config.getTunnels();
//                for (Tunnel tunnel : tunnels) {
//                    if (tunnel.getRemotePort() > 0) {
//                        NatMessage message = NatMessage.build();
//                        byte[] content = JsonUtil.serialize(tunnel).getBytes();
//                        message.setProtocol(ProtocolType.CONTROL.getCode());
//                        message.setType(ControlMessageType.REGISTER_TUNNEL.getCode());
//                        message.setBody(content);
//                        log.debug("Write message: {}", message);
//                        future.channel().writeAndFlush(message);
//                    }
//                }
            } else {
                future.channel().eventLoop().schedule(() -> {
                    try {
                        connect();
                    } catch (Exception e) {
                        log.debug("Connect error", e);
                        log.info("Connect failure, schedule try to  reconnect after {} seconds", config.getReconnectSeconds());
                    }
                }, config.getReconnectSeconds(), TimeUnit.SECONDS);
            }
        });
        return f;
    }

    public void reconnect() {
        run();
    }

    public void run() {
        Runnable runnable = () -> {
            NatClientConfigProperties config = null;
            try {
                config = ClientContext.getConfig();
                connect().channel().closeFuture().sync();
            } catch (Exception e) {
                log.debug("Connect error", e);
                log.info("Connect failure, try to reconnect after {} seconds", config.getReconnectSeconds());
                try {
                    TimeUnit.SECONDS.sleep(config.getReconnectSeconds());
                } catch (InterruptedException e1) {
                    log.error("Thread sleep InterruptedException", e);
                }
                run();
            }
        };
        new Thread(runnable).start();
    }
}
