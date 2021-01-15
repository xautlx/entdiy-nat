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

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ProxyChannelSource {
    // key为目标host，value为目标host的连接池
    public static Map<String, ProxyChannelPool> proxyChannelPoolMap = Maps.newConcurrentMap();

    public static void init(Channel controlChannel, NatClient natClient) {
        proxyChannelPoolMap.put(natClient.getClient(), new ProxyChannelPool(controlChannel, natClient));
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
        private volatile LocalDateTime lastBatchReqProxyTime = null;
        private volatile AtomicInteger acquiringCount = new AtomicInteger();
        private volatile AtomicInteger count = new AtomicInteger();
        private final Deque<Channel> deque = PlatformDependent.newConcurrentDeque();

        public ProxyChannelPool(Channel controlChannel, NatClient natClient) {
            this.controlChannel = controlChannel;
            this.natClient = natClient;
            batchReqProxy(natClient.getPoolCoreSize());
        }

        private void batchReqProxy(int newCount) {
            lastBatchReqProxyTime = LocalDateTime.now();
            log.debug("BatchReqProxy times: {}", count.incrementAndGet());
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
            deque.offerLast(channel);
            log.debug("Proxy channels pool free after add: {}", deque.size());
            channel.closeFuture().addListener((ChannelFutureListener) t -> {
                Channel closeProxyChannel = t.channel();
                log.info("Disconnect to proxy channel: {}", closeProxyChannel);
                Channel remoteChannel = RemotePortHandler.removeChannelMapping(closeProxyChannel);
                if (remoteChannel != null) {
                    log.info("Closing remote channel: {}", remoteChannel);
                    remoteChannel.close();
                }
            });

        }

        public Channel acquire() {
            int size = natClient.getPoolCoreSize();
            Channel validChannel = null;
            while (validChannel == null) {
                Channel channel = deque.pollLast();
                if (channel == null) {
                    try {
                        batchReqProxy(1);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        log.error("thread error", e);
                    }
                } else if (healthCheck.isHealthy(channel).getNow()) {
                    validChannel = channel;
                }
            }

            if (deque.size() < size
                    && (lastBatchReqProxyTime == null || lastBatchReqProxyTime.plusMinutes(1).isBefore(LocalDateTime.now()))) {
                batchReqProxy(size);
            }
            log.debug("Proxy channels pool free after acquire: {}", deque.size());
            return validChannel;
        }
    }
}
