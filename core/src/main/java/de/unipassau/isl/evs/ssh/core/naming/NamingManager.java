package de.unipassau.isl.evs.ssh.core.naming;


import java.security.PublicKey;
import java.security.cert.Certificate;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

public class NamingManager extends AbstractComponent {

    private DeviceID localDeviceID;

    public DeviceID getLocalID() {
        return localDeviceID;
    }

    public DeviceID getMasterID(){
        //TODO implement
        return null;
    }

    public Certificate getMasterCert(){
        //TODO implement
        return null;
    }

    public PublicKey getPublicKey(DeviceID id) {
        //TODO implement
        return null;
    }

    public DeviceID getDeviceID(Certificate cert){
        //TODO implement
        return null;
    }

    public Certificate getCertificate(DeviceID id) {
        //TODO implement
        return null;
    }

    @Override
    public void init(Container container) {
        super.init(container);
        //TODO init mydeviceid
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
