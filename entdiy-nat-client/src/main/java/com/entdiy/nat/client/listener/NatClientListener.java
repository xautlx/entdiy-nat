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
package com.entdiy.nat.client.listener;

import com.entdiy.nat.client.ClientContext;
import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.client.handler.ClientControlHandler;
import com.entdiy.nat.common.codec.NatMessageDecoder;
import com.entdiy.nat.common.codec.NatMessageEncoder;
import com.entdiy.nat.common.constant.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NatClientListener {

    /**
     * 连接失败重连间隔秒数
     */
    private static final int RECONNECT_INTERVAL_SECONDS = 30;

    /**
     * 失败连接次数累加器，连接成功后重置0
     */
    private static int connectTimes = 0;

    /**
     * 重连定时作业服务
     */
    private ScheduledExecutorService pool = null;

    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static Bootstrap b = new Bootstrap();

    static {
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        //    p.addLast(sslCtx.newHandler(ch.alloc()));
                        p.addLast(new LoggingHandler());
                        p.addLast(new IdleStateHandler(60, 80, 120));
                        p.addLast(new DelimiterBasedFrameDecoder(10240, Constant.DELIMITER));
                        p.addLast(new NatMessageDecoder());
                        p.addLast(new NatMessageEncoder());
                        p.addLast(new ClientControlHandler());
                    }
                });
    }


    public void run() {
        NatClientConfigProperties config = ClientContext.getConfig();
        try {
            SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            if (connectTimes == 0) {
                log.debug("Start connect to server {}:{}", config.getServerAddr(), config.getPort());
            } else {
                log.debug("Start {} times reconnect to server {}:{}", connectTimes, config.getServerAddr(), config.getPort());
            }
            connectTimes++;
            ChannelFuture f = b.connect(config.getServerAddr(), config.getPort()).sync();
            f.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("Success connect to server {}:{}", config.getServerAddr(), config.getPort());
                    connectTimes = 0;
                } else {
                    log.warn("Fail connect to server {}:{}", config.getServerAddr(), config.getPort());
                    scheduleReconnect();
                }
            });
            f.channel().closeFuture().addListener((ChannelFutureListener) t -> {
                log.warn("Disconnect to server {}:{}", config.getServerAddr(), config.getPort());
                scheduleReconnect();
            });
        } catch (Exception e) {
            log.warn("Exception connect to server {}:{}", config.getServerAddr(), config.getPort());
            log.debug("Exception connect to server", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (pool == null) {
            pool = Executors.newScheduledThreadPool(1);
        }
        log.warn("Schedule to reconnect after {} seconds", RECONNECT_INTERVAL_SECONDS);
        pool.schedule(() -> run(), RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void shutdown() {
        group.shutdownGracefully();
        log.info("Done shutdownGracefully!");
    }
}
