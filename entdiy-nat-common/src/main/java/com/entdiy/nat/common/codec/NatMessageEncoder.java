package com.entdiy.nat.common.codec;

import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatMessageEncoder extends MessageToByteEncoder<NatMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NatMessage msg, ByteBuf out) throws Exception {
        log.debug("Encode message for channel: {}, message: {}", ctx.channel(), msg);
        out.writeInt(msg.getCrcCode());
        out.writeInt(msg.getLength());
        out.writeByte(msg.getProtocol());
        out.writeByte(msg.getType());
        out.writeBytes(msg.getBody());
    }

}
