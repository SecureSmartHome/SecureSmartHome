package de.unipassau.isl.evs.ssh.master.network;

import java.net.InetSocketAddress;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;

public class ServerBroadcastHandler extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        if ("REQUEST".equals(msg.content().toString(CharsetUtil.UTF_8))) {
            String port = "13131"; //TODO get preferred port from server
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("RESPONSE:" + port, CharsetUtil.UTF_8),
                    new InetSocketAddress(CoreConstants.BROADCAST_ADDRESS, DEFAULT_PORT))).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                }
            });
        }//else discard message
    }
}
