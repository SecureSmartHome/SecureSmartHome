package de.unipassau.isl.evs.ssh.core.network.handler;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.network.Client;
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

    private final Client client;

    public ClientBroadcastHandler(Client client) {
        this.client = client;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        String messageData = msg.content().toString(CharsetUtil.UTF_8);
        if (messageData.startsWith("RESPONSE")) {
            String address = msg.sender().getAddress().toString();
            int port = Integer.parseInt(messageData.substring(0, 8));
            client.receivedUDPResponse(address, port);
        }//else discard message
    }
}
