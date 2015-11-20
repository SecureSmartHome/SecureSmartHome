package de.unipassau.isl.evs.ssh.core.network.handler;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

public class ClientBroadcastHandler extends MessageToMessageDecoder<DatagramPacket> {
    /**
     * SharedPreferences to load, save and edit key-value sets.
     */
//    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences();

    /**
     * SharedPreferences editor to edit the key-value sets.
     */
//    SharedPreferences.Editor editor = prefs.edit();
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        if ("RESPONSE".equals(msg.content().toString(CharsetUtil.UTF_8))) {
//            TODO get context for sharedpreferences
//            set TIMEOUTS_IN_A_ROW to 0.
//            set PREF_HOST to address.
//            String address = msg.sender().getAddress().toString();

        }//else discard message
        ctx.close();
    }
}
