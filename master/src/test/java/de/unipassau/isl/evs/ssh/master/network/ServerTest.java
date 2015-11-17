package de.unipassau.isl.evs.ssh.master.network;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServerTest {

    @Test
    public void testServerConnect() throws InterruptedException, IOException {
        SimpleContainer container = new SimpleContainer();
        container.register(Server.KEY, new Server());

        Server server = container.get(Server.KEY);
        assertNotNull(server.getAddress());
        assertTrue(server.isChannelOpen());
        assertTrue(server.isExecutorAlive());

        {
            Socket client1 = new Socket("localhost", ((InetSocketAddress) server.getAddress()).getPort());
            assertTrue(client1.isConnected());
            assertEquals(1, server.getActiveChannels().size());
            client1.close();
            assertFalse(client1.isConnected());
            assertEquals(0, server.getActiveChannels().size());
        }

        container.shutdown();

        server.awaitShutdown();

        assertFalse(server.isChannelOpen());
        assertFalse(server.isExecutorAlive());
    }
}