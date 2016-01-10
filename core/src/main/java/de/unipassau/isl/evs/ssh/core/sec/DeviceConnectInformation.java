package de.unipassau.isl.evs.ssh.core.sec;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;

import static io.netty.handler.codec.base64.Base64.decode;
import static io.netty.handler.codec.base64.Base64.encode;

/**
 * //TODO add javadoc
 *
 * @author Niko Fink
 */
public class DeviceConnectInformation implements Serializable {
    private static final String TAG = DeviceConnectInformation.class.getSimpleName();
    private static final QRCodeWriter writer = new QRCodeWriter();
    public static final int ADDRESS_LENGTH = 4;
    public static final int TOKEN_LENGTH = 35;
    public static final int TOKEN_BASE64_LENGTH = encodeToken(new byte[TOKEN_LENGTH]).length();
    private static final int DATA_LENGTH = 4 + 2 + TOKEN_BASE64_LENGTH + DeviceID.ID_LENGTH;
    private static final int BASE64_FLAGS = android.util.Base64.NO_WRAP;

    private final InetAddress address;
    private final int port;
    private final DeviceID id;
    private final byte[] token;

    public DeviceConnectInformation(InetAddress address, int port, DeviceID id, byte[] token) {
        if (address.getAddress().length != ADDRESS_LENGTH) {
            throw new IllegalArgumentException("illegal address length");
        }
        this.address = address;
        this.port = port;
        this.id = id;
        if (token.length != TOKEN_LENGTH) {
            throw new IllegalArgumentException("illegal token length");
        }
        this.token = token;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public DeviceID getID() {
        return id;
    }

    public byte[] getToken() {
        return token;
    }

    public static DeviceConnectInformation fromDataString(String data) throws IOException {
        final ByteBuf base64 = UnpooledByteBufAllocator.DEFAULT.heapBuffer(data.length());
        ByteBufUtil.writeAscii(base64, data);
        final ByteBuf byteBuf = decode(base64);
        if (byteBuf.readableBytes() != DATA_LENGTH) {
            throw new IOException("too many bytes encoded");
        }

        final byte[] addressData = new byte[ADDRESS_LENGTH];
        byteBuf.readBytes(addressData);
        final InetAddress address = InetAddress.getByAddress(addressData);
        final int port = byteBuf.readUnsignedShort();
        final byte[] idData = new byte[DeviceID.ID_LENGTH];
        byteBuf.readBytes(idData);
        final DeviceID id = new DeviceID(idData);
        final byte[] encodedToken = new byte[TOKEN_BASE64_LENGTH];
        byteBuf.readBytes(encodedToken);
        final byte[] token = decodeToken(new String(encodedToken));

        return new DeviceConnectInformation(address, port, id, token);
    }

    public String toDataString() {
        final ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(DATA_LENGTH, DATA_LENGTH);
        byteBuf.writeBytes(address.getAddress());
        byteBuf.writeShort(port);
        byteBuf.writeBytes(id.getIDBytes());
        byteBuf.writeBytes(encodeToken(token).getBytes());
        return encode(byteBuf).toString(Charsets.US_ASCII);
    }

    public BitMatrix toQRBitMatrix() throws WriterException {
        return writer.encode(toDataString(), BarcodeFormat.QR_CODE, 0, 0);
    }

    public Bitmap toQRBitmap(Bitmap.Config config, int onColor, int offColor) throws WriterException {
        BitMatrix matrix = toQRBitMatrix();
        final int width = matrix.getWidth();
        final int height = matrix.getHeight();
        final int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? onColor : offColor;
            }
        }

        Bitmap image = Bitmap.createBitmap(width, height, config);
        image.setPixels(pixels, 0, width, 0, 0, width, height);
        return image;
    }

    private static SecureRandom random = null;

    public static byte[] getRandomToken() {
        byte[] token = new byte[TOKEN_LENGTH];
        if (random == null) {
            random = new SecureRandom();
        }
        random.nextBytes(token);
        return token;
    }

    public static String encodeToken(byte[] token) {
        return Base64.encodeToString(token, BASE64_FLAGS);
    }

    public static byte[] decodeToken(String token) {
        if (Strings.isNullOrEmpty(token)) return new byte[0];
        return Base64.decode(token, BASE64_FLAGS);
    }

    /**
     * Android has no uniform way of finding the IP address of the local device, so this first queries the WifiManager
     * and afterwards tries to find a suitable NetworkInterface. If both fails, returns 0.0.0.0.
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public static InetAddress findIPAddress(Context context) {
        WifiManager wifiManager = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null) {
            try {
                return InetAddress.getByName(
                        Formatter.formatIpAddress(connectionInfo.getIpAddress())
                );
            } catch (UnknownHostException e) {
                Log.wtf(TAG, "Android API couldn't resolve the IP Address of the local device", e);
            }
        }
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == ADDRESS_LENGTH) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            Log.wtf(TAG, "Android API couldn't query own network interfaces", e);
        }
        try {
            return InetAddress.getByAddress(new byte[ADDRESS_LENGTH]);
        } catch (UnknownHostException e) {
            throw new AssertionError("Could not resolve IP 0.0.0.0", e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("address", address)
                .add("port", port)
                .add("id", id)
                .add("token", encodeToken(token))
                .toString();
    }
}
