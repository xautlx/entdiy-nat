package com.entdiy.nat.server.handler;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.handler.NatCommonHandler;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.StartProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RemotePortHandler extends NatCommonHandler {

    private String clientToken;
    private String url;
    private Channel controlChannel;
    private boolean init = false;

    public RemotePortHandler(String clientToken, String url, Channel controlChannel) {
        this.clientToken = clientToken;
        this.url = url;
        this.controlChannel = controlChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read TCP message from external...");
        if (msg == null) {
            return;
        }

        if (init == false) {
            StartProxyMessage respBody = new StartProxyMessage();
            respBody.setUrl(url);
            byte[] respBodyContent = JsonUtil.serialize(respBody).getBytes();

            NatMessage respMessage = NatMessage.build();
            respMessage.setType(ControlMessageType.StartProxy.getCode());
            respMessage.setProtocol(ProtocolType.PROXY.getCode());
            respMessage.setBody(respBodyContent);

            Channel proxyChannel = ServerControlHandler.getProxyChannelByClientToken(clientToken);
            proxyChannel.writeAndFlush(respMessage);
        }
    }

}
