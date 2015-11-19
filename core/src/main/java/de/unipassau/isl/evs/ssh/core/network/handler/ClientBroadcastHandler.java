package de.unipassau.isl.evs.ssh.core.network.handler;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

public class ClientBroadcastHandler extends MessageToMessageDecoder<DatagramPacket> {
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        if ("RESPONSE".equals(msg.content().toString(CharsetUtil.UTF_8))) {
            //TODO change timeoutsInARow, also with sharedpreferences?
            //setTimeoutsInARow(0);
            String address = msg.sender().getAddress().toString();
            //TODO change ip in the sharedpreferences
        }//else discard message

    }
}
