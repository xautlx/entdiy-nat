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
