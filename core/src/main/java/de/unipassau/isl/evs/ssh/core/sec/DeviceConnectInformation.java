package de.unipassau.isl.evs.ssh.core.sec;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.SecureRandom;

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
    private static final QRCodeWriter writer = new QRCodeWriter();
    private static final int ADDRESS_LENGTH = 4;
    public static final int TOKEN_LENGTH = 35;
    public static final int TOKEN_BASE64_LENGTH = encodeToken(new byte[TOKEN_LENGTH]).length();
    private static final int DATA_LENGTH = 4 + 2 + TOKEN_BASE64_LENGTH + DeviceID.ID_LENGTH;
    private static final int BASE64_FLAGS = android.util.Base64.NO_WRAP;

    private final Inet4Address address;
    private final int port;
    private final DeviceID id;
    private final byte[] token;

    public DeviceConnectInformation(Inet4Address address, int port, DeviceID id, byte[] token) {
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

    public Inet4Address getAddress() {
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
        final Inet4Address address = (Inet4Address) InetAddress.getByAddress(addressData);
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
