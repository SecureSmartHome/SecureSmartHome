package de.unipassau.isl.evs.ssh.core.network.handler;

import android.util.Log;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

public class PipelinePlug extends ChannelHandlerAdapter {
    private static final String TAG = PipelinePlug.class.getSimpleName();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Log.e(TAG, (msg != null ? msg.getClass().getName() : "null") + " packet reached end of pipeline: "
                + String.valueOf(msg));
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.e(TAG, "Exception reached end of pipeline: ", cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Log.w(TAG, (evt != null ? evt.getClass().getName() : "null") + " event reached end of pipeline: "
                + String.valueOf(evt));
        ReferenceCountUtil.release(evt);
    }
}
