/**
 * Copyright @ 2020-2020 EntDIY NAT (like Ngrok) based on Netty
 *
 * Author: Li Xia, E-Mail: xautlx@hotmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.entdiy.nat.client.handler;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.NewTunnelMessage;
import com.entdiy.nat.common.model.ReqTunnelMessage;
import com.entdiy.nat.common.model.Tunnel;
import com.entdiy.nat.common.util.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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

    public ClientTunnelHandler(String clientToken,Tunnel tunnel) {
        this.clientToken=clientToken;
        this.tunnel = tunnel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Assert.hasText(tunnel.getCode(), "Tunnel 'code' required");
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
        }
    }

    public static Tunnel getByUrl(String url) {
        return urlTunnelMapping.get(url);
    }
}
