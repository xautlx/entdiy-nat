package com.entdiy.nat.common.codec;

import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class NatMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) throws Exception {
        int crcCode = in.readInt();
        if (crcCode == NatMessage.CRC_CODE) {
            NatMessage message =NatMessage.build();
            int length = in.readInt();
            message.setLength(length);
            byte protocol = in.readByte();
            message.setProtocol(protocol);
            byte type = in.readByte();
            message.setType(type);
            byte[] body = new byte[length];
            in.readBytes(body);
            message.setBody(body);
            out.add(message);
        }
    }

}
