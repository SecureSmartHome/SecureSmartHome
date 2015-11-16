package de.unipassau.isl.evs.ssh.core.util;

public final class UtilityFunctions {

    /**
     * Hexlify an byte array.
     * @param b byte array to hexlify.
     * @return Hexlified version of the byte array.
     */
    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

}
