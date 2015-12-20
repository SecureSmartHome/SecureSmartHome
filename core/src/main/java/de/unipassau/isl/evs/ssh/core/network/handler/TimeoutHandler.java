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
