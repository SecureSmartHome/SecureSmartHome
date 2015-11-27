package de.unipassau.isl.evs.ssh.core.network.handler;

import java.io.Serializable;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * TimeoutHandler class is part of the netty pipeline. Handles timeouts by sending {@link PingMessage}.
 *
 * @author Phil
 */
public class TimeoutHandler extends ChannelHandlerAdapter {
    /**
     * Handles received timeout event.
     * Sends a {@link PingMessage}.
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new PingMessage());
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
        if (!(msg instanceof PingMessage)) {
            super.channelRead(ctx, msg);
        } // else discard message
        ctx.close();
    }

    /**
     * Empty class used for simple ping messages.
     */
    public static class PingMessage implements Serializable {

    }
}
