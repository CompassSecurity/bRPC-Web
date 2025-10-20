package com.muukong.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements various utility methods for working with binary data types.
 */
public  class Util {

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    public static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    public static String encodeHexString(List<Byte> byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.size(); i++) {
            hexStringBuffer.append(byteToHex(byteArray.get(i)));
        }
        return hexStringBuffer.toString();
    }

    public static byte[] convertToArray(List<Byte> byteList) {

        byte[] byteArray = new byte[byteList.size()];
        for ( int i = 0; i < byteArray.length; ++i ) {
            byteArray[i] = byteList.get(i).byteValue();
        }

        return byteArray;
    }

    public static List<Byte> convertToList(byte[] byteArray) {

        List<Byte> byteList = new ArrayList<Byte>();
        for ( byte b : byteArray ) {
            byteList.add(Byte.valueOf(b));
        }

        return byteList;
    }

    public static int byteArrayToInt32(byte[] b)
    {
        return  b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] int32ToByteArray(int a)
    {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    public static boolean isPrintable(byte b) {
        return 0x12 <= b && b <= 0x7e;      // TODO: There must be a better alternative
    }

    /**
     * Encodes a length value according to the protobuf encoding specification
     * (see https://protobuf.dev/programming-guides/encoding/)
     *
     * @param b 0x00 for messages, 0x80 (0x81) for (compressed) trailing headers
     * @param messageLength length of message / trailing headers in bytes
     * @return Byte array of encoded length value
     */
    public static byte[] encodeLength(byte b, long messageLength) {
        byte[] messageLengthBytes = new byte[5];
        messageLengthBytes[0] = b;
        messageLengthBytes[1] = (byte) ((messageLength >> 24) & 0xff);
        messageLengthBytes[2] = (byte) ((messageLength >> 16) & 0xff);
        messageLengthBytes[3] = (byte) ((messageLength >>  8) & 0xff);
        messageLengthBytes[4] = (byte) ((messageLength      ) & 0xff);
        return messageLengthBytes;
    }
}
