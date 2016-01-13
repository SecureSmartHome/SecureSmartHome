package de.unipassau.isl.evs.ssh.core.network.handler;

import android.test.InstrumentationTestCase;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;

public class CrypterTest extends InstrumentationTestCase {
    private Container container;
    private EmbeddedChannel channel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        container = new SimpleContainer();
        KeyStoreController keyStoreController = new KeyStoreController();
        container.register(ContainerService.KEY_CONTEXT, new ContainerService.ContextComponent(getInstrumentation().getContext()));
        container.register(KeyStoreController.KEY, keyStoreController);

        final Encrypter encrypter = new Encrypter(keyStoreController.getOwnCertificate().getPublicKey());
        final Decrypter decrypter = new Decrypter(keyStoreController.getOwnPrivateKey());
        channel = new EmbeddedChannel(encrypter, decrypter);
    }

    @Override
    protected void tearDown() throws Exception {
        container.shutdown();

        channel.runPendingTasks();
        channel.checkException();
        assertTrue(channel.inboundMessages().isEmpty());
        assertTrue(channel.outboundMessages().isEmpty());

        super.tearDown();
    }

    public void testRoundtrip() {
        ByteBuf buf = channel.alloc().buffer();
        for (int c : new int[]{1, 1, 5, 10, 50, 100, 500, 1000, 5000, 10000}) {
            buf.capacity(c);
            while (buf.writableBytes() > 0) {
                buf.writeByte(c);
            }

            channel.writeOutbound(buf.duplicate().retain());
            for (ByteBuf msg; (msg = channel.readOutbound()) != null; ) {
                assertNotSame(buf, msg);
                channel.writeInbound(msg);
            }
            assertEquals(buf, channel.readInbound());
        }
        ReferenceCountUtil.release(buf);
    }
}