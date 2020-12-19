package com.entdiy.nat.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FetchDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private String clientAddr;
    private Channel channel;

    FetchDataHandler(String clientAddr, Channel channel) {
        this.clientAddr = clientAddr;
        this.channel = channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        log.info("FetchData write message to remote address {}, {} bytes", channel.remoteAddress(), byteBuf.readableBytes());
        channel.writeAndFlush(byteBuf.copy());
    }
}
