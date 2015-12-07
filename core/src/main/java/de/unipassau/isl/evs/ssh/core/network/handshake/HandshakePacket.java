package de.unipassau.isl.evs.ssh.core.network.handshake;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.BuildConfig;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

public abstract class HandshakePacket implements Serializable {
    public static final int PROTOCOL_VERSION = 2;

    /**
     * The protocol version of this packet
     */
    private final int protocolVersion = PROTOCOL_VERSION;
    /**
     * Used for debugging purposes to easier identify HandshakePackets in network dumps
     */
    private final String packetIdentifier = getClass().getSimpleName();

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        final List<Field> fields = Arrays.asList(getClass().getFields());
        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field lhs, Field rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        for (Field field : fields) {
            Object value;
            try {
                value = field.get(this);
            } catch (IllegalAccessException e) {
                value = e;
            }
            helper.add(field.getName(), value);
        }
        return helper.toString();
    }

    public static class SerializableBuildConfig implements Serializable {
        public final boolean debug;
        public final String application_id;
        public final String build_type;
        public final String flavor;
        public final int version_code;
        public final String version_name;

        public SerializableBuildConfig(Class<?> clazz) throws NoSuchFieldException, IllegalAccessException {
            debug = (boolean) clazz.getField("DEBUG").get(null);
            application_id = (String) clazz.getField("APPLICATION_ID").get(null);
            build_type = (String) clazz.getField("BUILD_TYPE").get(null);
            flavor = (String) clazz.getField("FLAVOR").get(null);
            version_code = (int) clazz.getField("VERSION_CODE").get(null);
            version_name = (String) clazz.getField("VERSION_NAME").get(null);
        }

        public static SerializableBuildConfig getInstance() {
            for (String s : new String[]{"app", "slave", "master", "core"}) {
                try {
                    final String n = BuildConfig.class.getName().replaceAll("core", s);
                    final Class<?> clazz = Class.forName(n);
                    return new SerializableBuildConfig(clazz);
                } catch (ReflectiveOperationException ignore) {
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("debug", debug)
                    .add("application_id", application_id)
                    .add("build_type", build_type)
                    .add("flavor", flavor)
                    .add("version_code", version_code)
                    .add("version_name", version_name)
                    .toString();
        }
    }

    public static class ClientHello extends HandshakePacket {
        public final X509Certificate clientCertificate;
        public final DeviceID masterID;
        public final SerializableBuildConfig buildConfig;
        public final byte[] challenge;

        public ClientHello(X509Certificate clientCertificate, DeviceID masterID, byte[] challenge) {
            this.clientCertificate = clientCertificate;
            this.masterID = masterID;
            this.buildConfig = SerializableBuildConfig.getInstance();
            this.challenge = challenge;
        }
    }

    public static class ServerHello extends HandshakePacket {
        public final X509Certificate serverCertificate;
        public final SerializableBuildConfig buildConfig;
        public final byte[] response;

        public ServerHello(X509Certificate serverCertificate, byte[] response) {
            this.serverCertificate = serverCertificate;
            this.buildConfig = SerializableBuildConfig.getInstance();
            this.response = response;
        }
    }

    public static class ClientRegistration extends HandshakePacket {
        public final X509Certificate clientCertificate;
        public final byte[] token;

        public ClientRegistration(X509Certificate clientCertificate, byte[] token) {
            this.clientCertificate = clientCertificate;
            this.token = token;
        }
    }

    public static class ServerRegistrationResponse extends HandshakePacket {
        public final boolean success;
        public final String message;
        public final X509Certificate serverCertificate;

        public ServerRegistrationResponse(boolean success, String message, X509Certificate serverCertificate) {
            this.success = success;
            this.message = message;
            this.serverCertificate = serverCertificate;
        }
    }
}
