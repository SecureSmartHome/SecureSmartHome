package de.unipassau.isl.evs.ssh.core.network;

import junit.framework.TestCase;

import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

public class ClientTest extends TestCase {

    public void testClientStart() throws InterruptedException {
        SimpleContainer container = new SimpleContainer();
        Client client = new Client();
        container.register(Client.KEY, client);
        client.awaitShutdown();
        container.shutdown();
    }

    public void testClient() throws InterruptedException {
        SimpleContainer container = new SimpleContainer();
        Client client = new Client();
        container.register(Client.KEY, client);

        assertNotNull(client.getAddress());
        assertTrue(client.isChannelOpen());
        assertTrue(client.isExecutorAlive());

        client.awaitShutdown();
        container.shutdown();
        assertFalse(client.isChannelOpen());
        assertFalse(client.isExecutorAlive());
    }
}