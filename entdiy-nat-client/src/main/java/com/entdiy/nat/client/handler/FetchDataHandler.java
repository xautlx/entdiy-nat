package com.entdiy.nat.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FetchDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Channel proxyChannel;

    FetchDataHandler(Channel proxyChannel) {
        this.proxyChannel = proxyChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        log.info("Write message back to proxy channel: {}, {} bytes", proxyChannel, byteBuf.readableBytes());
        proxyChannel.writeAndFlush(byteBuf.copy());
    }
}
