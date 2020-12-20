package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.ReqProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class HttpHandler extends NatCommonHandler {

    private String clientToken;
    private String url;
    private boolean init = false;
    private Channel controlChannel;

    private BiMap<String, Channel> clientAddrChannelMapping = HashBiMap.create();

    public HttpHandler(String clientToken, String url, Channel controlChannel) {
        this.clientToken = clientToken;
        this.url = url;
        this.controlChannel = controlChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientAddr = ctx.channel().remoteAddress().toString();
        log.debug("New data transfer connection coming: {}", clientAddr);
        clientAddrChannelMapping.put(clientAddr, ctx.channel());

        NatMessage reqProxyMessage = NatMessage.build();
        reqProxyMessage.setType(ControlMessageType.ReqProxy.getCode());
        reqProxyMessage.setProtocol(ProtocolType.CONTROL.getCode());
        reqProxyMessage.setBody(JsonUtil.serialize(new ReqProxyMessage()).getBytes());
        log.debug("Writing message: {}", reqProxyMessage);
        controlChannel.writeAndFlush(reqProxyMessage);

//        StartProxyMessage respBody = new StartProxyMessage();
//        respBody.setUrl(this.url);
//        respBody.setClientAddr(clientAddr);
//        byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();
//
//        NatMessage respMessage = NatMessage.build();
//        respMessage.setType(ProxyMessageType.StartProxy.getCode());
//        respMessage.setProtocol(ProtocolType.PROXY.getCode());
//        respMessage.setBody(respBodyContent);
//        proxyChannel.writeAndFlush(respMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read TCP message from external...");
        if (msg == null) {
            return;
        }
//        ProxyingMessage respBody = new ProxyingMessage();
//        respBody.setUrl(this.url);
//        respBody.setClientAddr(ctx.channel().remoteAddress().toString());
//        respBody.setData(((ByteBuf)msg).readByte());
//        byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();
//
//        NatMessage respMessage = NatMessage.build();
//        respMessage.setType(ProxyMessageType.StartProxy.getCode());
//        respMessage.setProtocol(ProtocolType.PROXY.getCode());
//        respMessage.setBody(respBodyContent);
//        proxyChannel.writeAndFlush(respMessage);
        ByteBuf byteBuf = (ByteBuf) msg;
//        log.info("Write message to proxy channel: {}, data length: {}", proxyChannel.remoteAddress(), byteBuf.readableBytes());
//        proxyChannel.writeAndFlush(msg);
    }

}
