/**
 * Copyright @ 2020-2020 EntDIY NAT (like Ngrok) based on Netty
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
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatServerListener extends NatCommonListener {

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workGroup = new NioEventLoopGroup();
    private ServerBootstrap b = new ServerBootstrap();

    public void run() {
        try {
            NatServerConfigProperties config = ServerContext.getConfig();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LoggingHandler());
                            p.addLast(new NatMessageDecoder());
                            p.addLast(new NatMessageEncoder());
                            p.addLast(new ServerControlHandler());
                        }
                    });

            ChannelFuture f = b.bind(config.getTunnelAddr()).sync();
            log.info("Listening for control and proxy connections: {}", f.channel());
        } catch (Exception e) {
            log.error("ServerBootstrap bind error", e);
        } finally {

        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        log.info("Done shutdownGracefully!");
    }
}
