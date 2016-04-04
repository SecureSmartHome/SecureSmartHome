/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.network.handshake;

import android.support.annotation.Nullable;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.BuildConfig;

/**
 * Packets used by {@link de.unipassau.isl.evs.ssh.core.network.ClientHandshakeHandler} and {@link de.unipassau.isl.evs.ssh.master.network.ServerHandshakeHandler}
 * to perform the initial handshake and authentication.
 *
 * @author Niko Fink
 */
public abstract class HandshakePacket implements Serializable {
    /**
     * Used by peers to check if they talk a compatible version of the handshake protocol
     */
    public static final int PROTOCOL_VERSION = 3;

    /**
     * Used for debugging purposes to easier identify HandshakePackets in network dumps
     */
    private final String packetIdentifier = getClass().getSimpleName();

    public abstract String toString();

    /**
     * Initial HandshakePacket sent by Client and afterwards by Server containing general information about the device
     * and its Certificate.
     */
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

        /**
         * Serializable pendant to {@link BuildConfig}
         */
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

            @Nullable
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

    /**
     * Challenge-Response packets used to proof that the peer owns the private key belonging to the certificate it
     * indicated in the {@link de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.Hello} packet.
     * Simply contains an error of random data that the other side must return in a signed packet.
     * <p/>
     * The order of packets is
     * <table>
     * <tr>
     * <th>Client</th>
     * <th></th>
     * <th>Server</th>
     * </tr>
     * <tr>
     * <td>challengeA</td>
     * <td>-></td>
     * <td></td>
     * </tr>
     * <tr>
     * <td></td>
     * <td><-</td>
     * <td>responseA, challengeB</td>
     * </tr>
     * <tr>
     * <td>responseB</td>
     * <td>-></td>
     * <td></td>
     * </tr>
     * </table>
     */
    public static class CHAP extends HandshakePacket {
        public static final int CHALLENGE_LENGTH = 32;

        @Nullable
        public final byte[] challenge;
        @Nullable
        public final byte[] response;

        public CHAP(@Nullable byte[] challenge, @Nullable byte[] response) {
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

    /**
     * <p>
     * Sent by the Server after the CHAP process to tell the Client whether he was authenticated or not.
     * </p>
     * <p>
     * A passiveRegistrationToken is sent if another Device, that was already connected to the Server,
     * scanned a QR Code from this connecting Client and contains the token from that QR Code.
     * This process is called passive, because the new Device doesn't register itself,
     * but is registered by another already known device.
     * This process is used for registering new Slaves and follows these steps:
     * <ol>
     * <li>Slave displays QR Code with his connection data and passive registration token</li>
     * <li>Slave starts UDP discovery, but Master doesn't respond to requests from unknown clients</li>
     * <li>User Device connected to the Master scans the QR Code to the Master and sends all data to the Master</li>
     * <li>Master registers new Slave and stores passive registration token</li>
     * <li>As the Slave is now known to the Master, the Master responds to its UDP discovery requests</li>
     * <li>The Slave connects to the newly found master</li>
     * <li>The Master sends the passive registration token obtained from the QR Code</li>
     * <li>The Slave only accepts its new master if the token matches with the one from its QR Code, otherwise it closes the connection</li>
     * </ol>
     * </p>
     *
     * @see de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ActiveRegistrationRequest
     */
    public static class ServerAuthenticationResponse extends HandshakePacket {
        public final boolean isAuthenticated;
        @Nullable
        public final String message;
        @Nullable
        public final byte[] passiveRegistrationToken;
        public final boolean isConnectionLocal;

        public ServerAuthenticationResponse(boolean isAuthenticated, @Nullable String message, @Nullable byte[] passiveRegistrationToken, boolean isConnectionLocal) {
            this.isAuthenticated = isAuthenticated;
            this.message = message;
            this.passiveRegistrationToken = passiveRegistrationToken;
            this.isConnectionLocal = isConnectionLocal;
        }

        public static ServerAuthenticationResponse authenticated(@Nullable String message, @Nullable byte[] passiveRegistrationToken, boolean isConnectionLocal) {
            return new ServerAuthenticationResponse(true, message, passiveRegistrationToken, isConnectionLocal);
        }

        public static ServerAuthenticationResponse unauthenticated(@Nullable String message) {
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

    /**
     * Sent by the Client to actively register himself to the master by sending the token the master displayed as part
     * of a QR-Code.
     * This process is called active, because the new Device registers itself to the Master.
     * This process is used for registering new UserDevices and follows these steps:
     * <ol>
     * <li>Master displays QR Code with his connection data and active registration token</li>
     * <li>Device scans QR Code and tries to connect to address obtained from QR Code</li>
     * <li>After initial handshake, Device sends this packet</li>
     * <li>If the sent token matches with the token from the QR Code, the Master accepts the new UserDevice,
     * otherwise he closes the connection</li>
     * </ol>
     *
     * @see de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ServerAuthenticationResponse
     */
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
}