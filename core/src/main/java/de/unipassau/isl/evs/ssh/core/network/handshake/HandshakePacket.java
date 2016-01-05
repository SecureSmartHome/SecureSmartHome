package de.unipassau.isl.evs.ssh.core.network.handshake;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.BuildConfig;

/**
 * TODO Niko: javadoc (Niko, 2016-01-05)
 *
 * @author Niko Fink
 */
public abstract class HandshakePacket implements Serializable {
    public static final int PROTOCOL_VERSION = 3;

    /**
     * Used for debugging purposes to easier identify HandshakePackets in network dumps
     */
    private final String packetIdentifier = getClass().getSimpleName();

    public abstract String toString();

    public static class Hello extends HandshakePacket {
        public final int protocolVersion = PROTOCOL_VERSION;
        public final SerializableBuildConfig buildConfig = SerializableBuildConfig.getInstance();
        public final X509Certificate certificate;
        public final boolean isMaster;

        public Hello(X509Certificate certificate, boolean isMaster) {
            this.certificate = certificate;
            this.isMaster = isMaster;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("protocolVersion", protocolVersion)
                    .add("buildConfig", buildConfig)
                    .add("certificate", certificate)
                    .add("isMaster", isMaster)
                    .toString();
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
    }

    public static class CHAP extends HandshakePacket {
        public static final int CHALLENGE_LENGTH = 32;

        public final byte[] challenge;
        public final byte[] response;

        public CHAP(byte[] challenge, byte[] response) {
            this.challenge = challenge;
            this.response = response;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("challenge", Arrays.toString(challenge))
                    .add("response", Arrays.toString(response))
                    .toString();
        }
    }

    public static class ActiveRegistrationRequest extends HandshakePacket {
        public final byte[] activeRegistrationToken;

        public ActiveRegistrationRequest(byte[] activeRegistrationToken) {
            this.activeRegistrationToken = activeRegistrationToken;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("activeRegistrationToken", Arrays.toString(activeRegistrationToken))
                    .toString();
        }
    }

    public static class ServerAuthenticationResponse extends HandshakePacket {
        public final boolean isAuthenticated;
        public final String message;
        public final byte[] passiveRegistrationToken;
        public final boolean isConnectionLocal;

        public ServerAuthenticationResponse(boolean isAuthenticated, String message, byte[] passiveRegistrationToken, boolean isConnectionLocal) {
            this.isAuthenticated = isAuthenticated;
            this.message = message;
            this.passiveRegistrationToken = passiveRegistrationToken;
            this.isConnectionLocal = isConnectionLocal;
        }

        public static ServerAuthenticationResponse authenticated(String message, byte[] passiveRegistrationToken, boolean isConnectionLocal) {
            return new ServerAuthenticationResponse(true, message, passiveRegistrationToken, isConnectionLocal);
        }

        public static ServerAuthenticationResponse unauthenticated(String message) {
            return new ServerAuthenticationResponse(false, message, null, false);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("isAuthenticated", isAuthenticated)
                    .add("message", message)
                    .add("passiveRegistrationToken", Arrays.toString(passiveRegistrationToken))
                    .add("isConnectionLocal", isConnectionLocal)
                    .toString();
        }
    }
}