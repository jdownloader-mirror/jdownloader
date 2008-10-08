//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.io.UnsupportedEncodingException;

public class JDHexUtils {

    static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };
    public static final String REGEX_FIND_ALL_HEX = "[[a-fA-F0-9]{2}]*?";
    public static final String REGEX_MATCH_ALL_HEX = "([[a-fA-F0-9]{2}]*?)";
    public static final String REGEX_HTTP_NEWLINE = JDHexUtils.getHexString("\r") + "{0,1}" + JDHexUtils.getHexString("\n") + "{0,1}";

    public static String toString(String hexString){
        if (hexString==null) return null;
        return new String(JDHexUtils.getByteArray(hexString));
    }

    public static byte[] getByteArray(String hexString) {
        if (hexString == null) { return null; }
        int length = hexString.length();
        byte[] buffer = new byte[(length + 1) / 2];
        boolean evenByte = true;
        byte nextByte = 0;
        int bufferOffset = 0;

        if (length % 2 == 1) {
            evenByte = false;
        }

        for (int i = 0; i < length; i++) {
            char c = hexString.charAt(i);
            int nibble; // A "nibble" is 4 bits: a decimal 0..15

            if (c >= '0' && c <= '9') {
                nibble = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                nibble = c - 'A' + 0x0A;
            } else if (c >= 'a' && c <= 'f') {
                nibble = c - 'a' + 0x0A;
            } else {
                throw new NumberFormatException("Invalid hex digit '" + c + "'.");
            }

            if (evenByte) {
                nextByte = (byte) (nibble << 4);
            } else {
                nextByte += (byte) nibble;
                buffer[bufferOffset++] = nextByte;
            }
            evenByte = !evenByte;
        }
        return buffer;
    }

    static public String getHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        try {
            return new String(hex, "ASCII");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }
    }

    static public String getHexString(String string) {
        try {
            if (string == null) { return null; }
            byte[] raw = string.getBytes("ASCII");
            byte[] hex = new byte[2 * raw.length];
            int index = 0;

            for (byte b : raw) {
                int v = b & 0xFF;
                hex[index++] = HEX_CHAR_TABLE[v >>> 4];
                hex[index++] = HEX_CHAR_TABLE[v & 0xF];
            }

            return new String(hex, "ASCII");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return null;
        }
    }
}