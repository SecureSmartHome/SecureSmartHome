package de.unipassau.isl.evs.ssh.core.sec;

import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import static org.junit.Assert.*;

public class KeyStoreControllerTest {

    @Test
    public void testCalculatePublicKeyFingerprint() throws Exception {
        KeyStoreController keyStoreController = new KeyStoreController();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        PublicKey pk = kp.getPublic();
        String fingerprint = keyStoreController.calculatePublicKeyFingerprint(pk);
        assertTrue(fingerprint.matches("[0-9a-f]+"));
    }
}