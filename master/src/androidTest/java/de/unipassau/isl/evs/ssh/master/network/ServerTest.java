package de.unipassau.isl.evs.ssh.master.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

public class ServerTest extends InstrumentationTestCase {
    public void testStartup() throws IOException, InterruptedException {
        SimpleContainer container = new SimpleContainer();
        addContext(container);
        addServer(container);
        shutdownServer(container);
    }

    public void testConnect() throws IOException, InterruptedException {
        SimpleContainer container = new SimpleContainer();
        addContext(container);
        Server server = addServer(container);
        try {
            Socket client1 = new Socket("localhost", ((InetSocketAddress) server.getAddress()).getPort());
            Thread.sleep(1000);

            assertTrue(client1.isConnected());
            assertEquals(1, server.getActiveChannels().size());

            client1.close();
            Thread.sleep(1000);

            assertFalse(client1.isConnected());
            assertEquals(0, server.getActiveChannels().size());
        } finally {
            shutdownServer(container);
        }
    }

    public void testConnectClient() throws IOException, InterruptedException {
        SimpleContainer container = new SimpleContainer();
        addContext(container);
        Server server = addServer(container);
        try {
            container.register(Client.KEY, new Client());
            Thread.sleep(1000);

            Client client = container.get(Client.KEY);
            assertTrue(client.isChannelOpen());
            assertTrue(client.isExecutorAlive());
            assertEquals(1, server.getActiveChannels().size());

            container.unregister(Client.KEY);
            client.awaitShutdown();
            Thread.sleep(1000);

            assertFalse(client.isChannelOpen());
            assertFalse(client.isExecutorAlive());
            assertEquals(0, server.getActiveChannels().size());
        } finally {
            shutdownServer(container);
        }
    }

    //private void testRoudtrip() {} //TODO test pipeline

    private void addContext(SimpleContainer container) {
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        SharedPreferences sharedPref = container.get(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(MasterConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        assertTrue(
                sharedPref.edit()
                        .clear()
                        .putString(CoreConstants.PREF_HOST, "localhost")
                        .commit()
        );
    }

    @NonNull
    private Server addServer(SimpleContainer container) {
        container.register(Server.KEY, new Server());

        Server server = container.get(Server.KEY);
        assertNotNull(server.getAddress());
        assertTrue(server.isChannelOpen());
        assertTrue(server.isExecutorAlive());
        return server;
    }

    private void shutdownServer(SimpleContainer container) throws InterruptedException {
        Server server = container.get(Server.KEY);
        container.shutdown();
        if (server != null) {
            server.awaitShutdown();
            assertFalse(server.isChannelOpen());
            assertFalse(server.isExecutorAlive());
        }
    }
}