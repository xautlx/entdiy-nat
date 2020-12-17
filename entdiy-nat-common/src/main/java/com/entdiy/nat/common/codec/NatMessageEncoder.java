package com.entdiy.nat.common.codec;

import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class NatMessageEncoder extends MessageToByteEncoder<NatMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NatMessage msg, ByteBuf out) throws Exception {
        out.writeInt(msg.getCrcCode());
        out.writeInt(msg.getLength());
        out.writeByte(msg.getProtocol());
        out.writeByte(msg.getType());
        out.writeBytes(msg.getBody());
    }

}
