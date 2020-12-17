package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.handler.NatCommonHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ServerProxyHandler extends NatCommonHandler {

    private String clientToken;
    private Channel controlChannel;

    public ServerProxyHandler(String clientToken, Channel controlChannel) {
        this.clientToken = clientToken;
        this.controlChannel=controlChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read TCP message from external...");
        if (msg == null) {
            return;
        }


    }

}
