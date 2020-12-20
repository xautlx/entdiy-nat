package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.constant.ProxyMessageType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.StartProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.support.ProxyChannelSource;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RemotePortHandler extends NatCommonHandler {



    private String clientToken;
    private String url;

    private static BiMap<String, Channel> clientAddrPublicChannelMapping = HashBiMap.create();
    private static BiMap<String, Channel> clientAddrProxyChannelMapping = HashBiMap.create();

    public RemotePortHandler(String clientToken, String url) {
        this.clientToken = clientToken;
        this.url = url;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientAddr = ctx.channel().remoteAddress().toString();
        log.debug("New data transfer connection coming: {}", clientAddr);
        clientAddrPublicChannelMapping.put(clientAddr, ctx.channel());

        Channel channel = ProxyChannelSource.acquire(clientToken);
        clientAddrProxyChannelMapping.put(clientAddr, channel);
        StartProxyMessage respBody = new StartProxyMessage();
        respBody.setUrl(url);
        respBody.setClientAddr(clientAddr);
        byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

        NatMessage respMessage = NatMessage.build();
        respMessage.setType(ProxyMessageType.StartProxy.getCode());
        respMessage.setProtocol(ProtocolType.PROXY.getCode());
        respMessage.setBody(respBodyContent);
        channel.writeAndFlush(respMessage);

        // 连接放回连接池，这里一定记得放回去
        //pool.release(channel);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read public message from external...");
        if (msg == null) {
            return;
        }

        String clientAddr = clientAddrPublicChannelMapping.inverse().get(ctx.channel());
        Channel proxyChannel = clientAddrProxyChannelMapping.get(clientAddr);
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("Write message to proxy channel: {}, data length: {}", proxyChannel.remoteAddress(), byteBuf.readableBytes());
        proxyChannel.writeAndFlush(msg);
    }

    public static Channel getPublicChannel(Channel proxyChannel) {
        String clientAddr = clientAddrProxyChannelMapping.inverse().get(proxyChannel);
        return clientAddrPublicChannelMapping.get(clientAddr);
    }
}
