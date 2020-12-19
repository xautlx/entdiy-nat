package com.entdiy.nat.common.codec;

import com.entdiy.nat.common.model.NatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NatMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) throws Exception {
        int crcCode = in.readableBytes() > 8 ? in.readInt() : -1;
        if (crcCode == NatMessage.CRC_CODE) {
            NatMessage message = NatMessage.build();
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
        } else {
            out.add(in);
        }
    }

}
