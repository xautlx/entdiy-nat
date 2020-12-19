package com.entdiy.nat.client.handler;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.NewTunnelMessage;
import com.entdiy.nat.common.model.ReqTunnelMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ClientTunnelHandler extends ChannelInboundHandlerAdapter {

    private static Map<String, Tunnel> reqIdTunnelMapping = new HashMap<>();
    private static Map<String, Tunnel> urlTunnelMapping = new HashMap<>();

    private Tunnel tunnel;
    private String clientToken;
    private ChannelFuture f;
    private static NioEventLoopGroup group = new NioEventLoopGroup(1);

    public ClientTunnelHandler(String clientToken,Tunnel tunnel) {
        this.clientToken=clientToken;
        this.tunnel = tunnel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Assert.hasText(tunnel.getName(), "Tunnel 'name' required");
        ReqTunnelMessage bodyMessage = new ReqTunnelMessage();
        bodyMessage.setClientToken(clientToken);
        String reqId = UUID.randomUUID().toString();
        reqIdTunnelMapping.put(reqId, tunnel);
        bodyMessage.setReqId(reqId);
        bodyMessage.setHostname(tunnel.getHost());
        if (tunnel.getRemotePort() != null) {
            bodyMessage.setRemotePort(tunnel.getRemotePort());
            bodyMessage.setProtocol(ProtocolType.TCP.name());
        }
        //TODO HTTP处理
        byte[] bodyContent = JsonUtil.serialize(bodyMessage).getBytes();

        NatMessage message = NatMessage.build();
        message.setProtocol(ProtocolType.CONTROL.getCode());
        message.setType(ControlMessageType.ReqTunnel.getCode());
        message.setBody(bodyContent);
        log.debug("Write message: {}", message);
        ctx.channel().writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        log.trace("ClientTunnel read message: {}", msg);
        if (msg == null) {
            return;
        }

        if (msg instanceof NatMessage) {
            try {
                NatMessage messageIn = (NatMessage) msg;
                log.debug("Read message: {}", messageIn);
                if (messageIn.getProtocol() == ProtocolType.CONTROL.getCode()) {
                    if (messageIn.getType() == ControlMessageType.NewTunnel.getCode()) {
                        String reqBodyString = messageIn.getBodyString();
                        NewTunnelMessage reqBody = JsonUtil.deserialize(reqBodyString, NewTunnelMessage.class);
                        urlTunnelMapping.put(reqBody.getUrl(), reqIdTunnelMapping.get(reqBody.getReqId()));
                        log.info("New tunnel created: {}", messageIn.getBodyString());
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            log.info("Proxy write message to local port " + f.channel().localAddress());
            ByteBuf byteBuf = (ByteBuf) msg;
            f.channel().writeAndFlush(byteBuf.copy());
        }
    }

    public static Tunnel getByUrl(String url) {
        return urlTunnelMapping.get(url);
    }
}
