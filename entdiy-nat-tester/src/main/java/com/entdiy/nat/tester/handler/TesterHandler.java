package com.entdiy.nat.tester.handler;

import com.entdiy.nat.common.handler.NatCommonHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TesterHandler extends NatCommonHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Handler channelActive: {}", ctx.channel());
        ctx.writeAndFlush("OK");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("Write tester message to channel: {}, data length: {}", ctx.channel(), byteBuf.readableBytes());
        ctx.channel().writeAndFlush(byteBuf.copy());
    }
}
