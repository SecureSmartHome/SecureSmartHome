package de.unipassau.isl.evs.ssh.core.network.handler;

import java.net.InetSocketAddress;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;

public class ServerBroadcastHandler extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        if ("REQUEST".equals(msg.content().toString(CharsetUtil.UTF_8))) {
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("RESPONSE", CharsetUtil.UTF_8),
                    new InetSocketAddress(CoreConstants.BROADCAST_ADDRESS, DEFAULT_PORT))).sync();
        }//else discard message
        ctx.close();
    }
}
