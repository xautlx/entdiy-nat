package com.entdiy.nat.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseDecoder;

import java.util.List;

public class NatHttpResponseDecoder extends HttpResponseDecoder {
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        super.decode(ctx, buffer, out);
    }
}
