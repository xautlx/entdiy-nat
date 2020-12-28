/**
 * Copyright @ 2020-2020 EntDIY-NAT (like Ngrok) based on Netty
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
package com.entdiy.nat.server.support;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.ReqProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.handler.RemotePortHandler;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ProxyChannelSource {
    // key为目标host，value为目标host的连接池
    public static Map<String, ProxyChannelPool> proxyChannelPoolMap = Maps.newConcurrentMap();

    public static void init(Channel controlChannel, NatClient natClient) {
        proxyChannelPoolMap.put(natClient.getClientToken(), new ProxyChannelPool(controlChannel, natClient));
    }

    public static void add(String key, Channel channel) {
        proxyChannelPoolMap.get(key).add(channel);
    }

    public static Channel acquire(String key) {
        return proxyChannelPoolMap.get(key).acquire();
    }

    @Slf4j
    public static class ProxyChannelPool {
        private Channel controlChannel;
        private NatClient natClient;
        private BlockingQueue<Channel> freeChannels = new LinkedBlockingQueue();

        public ProxyChannelPool(Channel controlChannel, NatClient natClient) {
            this.controlChannel = controlChannel;
            this.natClient = natClient;
            for (int i = 0; i < natClient.getPoolCoreSize(); i++) {
                acquireNew();
            }
            controlChannel.flush();
        }

        private void acquireNew() {
            NatMessage reqProxyMessage = NatMessage.build();
            reqProxyMessage.setType(ControlMessageType.ReqProxy.getCode());
            reqProxyMessage.setProtocol(ProtocolType.CONTROL.getCode());
            reqProxyMessage.setBody(JsonUtil.serialize(new ReqProxyMessage()).getBytes());
            log.debug("Writing message: {}", reqProxyMessage);
            controlChannel.write(reqProxyMessage);
        }

        private void debug() {
            log.debug("Proxy channels pool free: {}", freeChannels.size());
        }

        public synchronized void add(Channel channel) {
            log.debug("ProxyChannelPool add: {}, {}", natClient, channel);
            channel.closeFuture().addListener((ChannelFutureListener) t -> {
                Channel closeProxyChannel = t.channel();
                log.info("Disconnect to proxy channel: {}", closeProxyChannel);
                Channel publicChannel = RemotePortHandler.getPublicChannel(closeProxyChannel);
                if (publicChannel != null) {
                    log.info("Closing public channel: {}", publicChannel);
                    publicChannel.close();
                }
            });

            try {
                freeChannels.put(channel);
                debug();
            } catch (InterruptedException e) {
                throw new RuntimeException("freeChannels put error", e);
            }
        }

        public synchronized Channel acquire() {
            log.debug("ProxyChannelPool acquire: {}", natClient);
            int freeSize = freeChannels.size();
            //简单处理：如果可用连接数低于核心数则直接新增扩容核心数量连接
            if (freeSize <= natClient.getPoolCoreSize()) {
                for (int i = 0; i < natClient.getPoolCoreSize(); i++) {
                    acquireNew();
                }
                controlChannel.flush();
            }

            try {
                Channel channel = freeChannels.take();
                debug();
                return channel;
            } catch (InterruptedException e) {
                throw new RuntimeException("freeChannels take error", e);
            }
        }
    }
}
