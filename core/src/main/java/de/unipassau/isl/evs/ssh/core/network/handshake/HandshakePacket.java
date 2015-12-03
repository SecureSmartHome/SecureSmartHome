package de.unipassau.isl.evs.ssh.core.network.handshake;

import java.io.Serializable;
import java.security.cert.X509Certificate;

public abstract class HandshakePacket implements Serializable {
    public static final int PROTOCOL_VERSION = 1;

    /**
     * Used for debugging purposes to easier identify HandshakePackets in network dumps
     */
    private final String packetIdentifier = getClass().getSimpleName();
    private final int protocolVersion = PROTOCOL_VERSION;

    public static class SerializableBuildConfig implements Serializable {
        public final boolean debug;
        public final String application_id;
        public final String build_type;
        public final String flavor;
        public final int version_code;
        public final String version_name;

        public SerializableBuildConfig(String className) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
            final Class<?> clazz = Class.forName(className);
            debug = (boolean) clazz.getField("DEBUG").get(null);
            application_id = (String) clazz.getField("APPLICATION_ID").get(null);
            build_type = (String) clazz.getField("BUILD_TYPE").get(null);
            flavor = (String) clazz.getField("FLAVOR").get(null);
            version_code = (int) clazz.getField("VERSION_CODE").get(null);
            version_name = (String) clazz.getField("VERSION_NAME").get(null);
        }
    }

    public static class ClientHello extends HandshakePacket {
        public final X509Certificate clientCertificate;
        public final X509Certificate masterCertificate;
        public final SerializableBuildConfig buildConfig;

        public ClientHello(X509Certificate clientCertificate, X509Certificate masterCertificate) {
            this.clientCertificate = clientCertificate;
            this.masterCertificate = masterCertificate;
            SerializableBuildConfig bc;
            try {
                bc = new SerializableBuildConfig("de.unipassau.isl.evs.ssh.app.BuildConfig");
            } catch (ReflectiveOperationException e1) {
                try {
                    bc = new SerializableBuildConfig("de.unipassau.isl.evs.ssh.slave.BuildConfig");
                } catch (ReflectiveOperationException e2) {
                    bc = null;
                }
            }
            this.buildConfig = bc;
        }
    }

    public static class ServerHello extends HandshakePacket {
        public final X509Certificate serverCertificate;
        public final SerializableBuildConfig buildConfig;

        public ServerHello(X509Certificate serverCertificate) {
            this.serverCertificate = serverCertificate;
            SerializableBuildConfig bc;
            try {
                bc = new SerializableBuildConfig("de.unipassau.isl.evs.ssh.core.BuildConfig");
            } catch (ReflectiveOperationException e2) {
                bc = null;
            }
            this.buildConfig = bc;
        }
    }
}
