package com.entdiy.nat.server.support;

import com.entdiy.nat.common.constant.ControlMessageType;
import com.entdiy.nat.common.constant.ProtocolType;
import com.entdiy.nat.common.model.NatMessage;
import com.entdiy.nat.common.model.ReqProxyMessage;
import com.entdiy.nat.common.util.JsonUtil;
import com.entdiy.nat.server.handler.RemotePortHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    public static void release(String key, Channel channel) {
        proxyChannelPoolMap.get(key).release(channel);
    }

    @Slf4j
    public static class ProxyChannelPool {
        private Channel controlChannel;
        private NatClient natClient;
        private List<Channel> freeChannels = Lists.newArrayList();
        private List<Channel> usingChannels = Lists.newArrayList();

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
            log.debug("Proxy channels pool free: {}, using: {}", freeChannels.size(), usingChannels.size());
        }

        public synchronized void add(Channel channel) {
            channel.closeFuture().addListener((ChannelFutureListener) t -> {
                Channel closeProxyChannel = t.channel();
                log.info("Disconnect to proxy channel: {}", closeProxyChannel);
                usingChannels.remove(closeProxyChannel);
                freeChannels.remove(closeProxyChannel);
                Channel publicChannel = RemotePortHandler.getPublicChannel(closeProxyChannel);
                if (publicChannel != null) {
                    publicChannel.close();
                }
                debug();
            });
            freeChannels.add(channel);
            debug();
        }

        public synchronized Channel acquire() {
            Channel channel = null;
            int freeSize = freeChannels.size();
            if (freeSize > 0) {
                channel = freeChannels.remove(0);
                freeSize--;
            }
            //简单处理：如果可用连接数低于核心数则直接新增扩容核心数量连接
            if (freeSize < natClient.getPoolCoreSize()) {
                for (int i = 0; i < natClient.getPoolCoreSize(); i++) {
                    acquireNew();
                }
                controlChannel.flush();
            }
            usingChannels.add(channel);
            debug();
            return channel;
        }

        public synchronized void release(Channel channel) {
            usingChannels.remove(channel);
            freeChannels.add(channel);
            debug();
        }

        public synchronized void remove(Channel channel) {
            usingChannels.remove(channel);
            freeChannels.remove(channel);
            debug();
        }
    }
}
