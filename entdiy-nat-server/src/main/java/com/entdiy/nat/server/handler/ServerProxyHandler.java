package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.handler.NatCommonHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ServerProxyHandler extends NatCommonHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read proxy message...");
        if (msg == null) {
            return;
        }

        if (msg instanceof ByteBuf) {
            Channel publicChannel = RemotePortHandler.getPublicChannel(ctx.channel());
            ByteBuf byteBuf = (ByteBuf) msg;
            log.info("Write message to public channel: {}, data length: {}", publicChannel.remoteAddress(), byteBuf.readableBytes());
            publicChannel.writeAndFlush(msg);
        }
    }

}
