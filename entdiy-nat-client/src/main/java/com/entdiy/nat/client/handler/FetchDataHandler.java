package com.entdiy.nat.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FetchDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Channel channel;

    FetchDataHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        log.info("FetchData write message to remote address " + channel.remoteAddress());
        channel.writeAndFlush(byteBuf.copy());
    }
}
