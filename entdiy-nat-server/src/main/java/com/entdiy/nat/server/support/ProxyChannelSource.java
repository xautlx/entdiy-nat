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
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;

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
        private ChannelHealthChecker healthCheck = ChannelHealthChecker.ACTIVE;
        private Channel controlChannel;
        private NatClient natClient;
        private final Deque<Channel> deque = PlatformDependent.newConcurrentDeque();

        public ProxyChannelPool(Channel controlChannel, NatClient natClient) {
            this.controlChannel = controlChannel;
            this.natClient = natClient;
            batchReqProxy(natClient.getPoolCoreSize());
        }

        private void batchReqProxy(int newCount) {
            for (int i = 0; i < newCount; i++) {
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

        public void add(Channel channel) {
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
            synchronized (natClient) {
                deque.offerLast(channel);
                log.debug("Proxy channels pool free after add: {}", deque.size());
                natClient.notify();
            }
        }

        public Channel acquire() {
            synchronized (natClient) {
                Channel channel = null;
                log.debug("ProxyChannelPool acquire: {}", natClient);
                while (true) {
                    try {
                        channel = deque.pollLast();
                        if (channel != null && healthCheck.isHealthy(channel).getNow()) {
                            break;
                        }
                        if (deque.size() < natClient.getPoolCoreSize()) {
                            batchReqProxy(2);
                            natClient.wait(5000);
                        }
                    } catch (InterruptedException e) {
                        log.error("thread error", e);
                    }
                }
                log.debug("Proxy channels pool free after acquire: {}", deque.size());
                return channel;
            }
        }
    }
}
