package com.entdiy.nat.common.adapter;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.util.AttributeKey;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HandlerInChannel {

    private Channel channel;

    public HandlerInChannel(Channel channel) {
        AttributeKey<Map<String, ChannelHandler>> key = AttributeKey.valueOf("HandlerInChannel");
        pool = channel.attr(key).get();
        if (pool == null) {
            pool = new ConcurrentHashMap<>();
            channel.attr(key).set(pool);
        }
        this.channel = channel;
        reset();
    }

    private Map<String, ChannelHandler> pool;

    public ChannelHandler get(String name, Supplier<ChannelHandler> supplier) {
        if (!pool.containsKey(name)) {
            pool.put(name, supplier.get());
        }
        return pool.get(name);
    }

    public void clear() {
        if (pool != null) {
            Collection<ChannelHandler> channelColletions = pool.values();
            for (ChannelHandler handler : channelColletions) {
                if (handler instanceof HandlerDestory) {
                    HandlerDestory handlerDestory = (HandlerDestory) handler;
                    handlerDestory.destory();
                }
            }
            pool.clear();
        }
    }

    public void reset() {
        Collection<ChannelHandler> channelColletions = pool.values();
        for (ChannelHandler handler : channelColletions) {
            if (channel.pipeline().context(handler.getClass()) != null) {
                channel.pipeline().remove(handler.getClass());
                try {
                    Field field = ChannelHandlerAdapter.class.getDeclaredField("added");
                    field.setAccessible(true);
                    field.set(handler, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }
}
