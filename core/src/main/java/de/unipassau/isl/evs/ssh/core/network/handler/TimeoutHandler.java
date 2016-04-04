/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.network.handler;

import android.util.Log;

import java.io.Serializable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

/**
 * TimeoutHandler class is part of the netty pipeline. Handles timeouts by sending {@link PingMessage}.
 *
 * @author Phil Werli
 */
public class TimeoutHandler extends ChannelHandlerAdapter {
    private static final String TAG = TimeoutHandler.class.getSimpleName();

    private static final AttributeKey<Boolean> SEND_PINGS = AttributeKey.valueOf(TimeoutHandler.class, "SEND_PINGS");

    /**
     * Handles received timeout event.
     * Sends a {@link PingMessage}.
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                Log.d(TAG, "Connection timed out");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                if (getPingEnabled(ctx.channel())) {
                    ctx.writeAndFlush(new PingMessage());
                }
            }
        }
    }

    /**
     * Calls {@link ChannelHandlerAdapter#channelRead(ChannelHandlerContext, Object)} when the sent message
     * is not an object of
     * {@link de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler.PingMessage PingMessage} class.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PingMessage) {
            // discard message
            ReferenceCountUtil.release(msg);
        } else {
            // forward message
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Empty class used for simple ping messages.
     */
    public static class PingMessage implements Serializable {
    }

    public static void setPingEnabled(Channel ch, boolean enabled) {
        ch.attr(TimeoutHandler.SEND_PINGS).set(enabled);
    }

    public static boolean getPingEnabled(Channel ch) {
        final Attribute<Boolean> attr = ch.attr(SEND_PINGS);
        attr.setIfAbsent(false);
        return attr.get();
    }
}
