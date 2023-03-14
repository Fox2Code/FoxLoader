package javax.xml.bind;

import java.text.SimpleDateFormat;
import java.util.Base64;

/**
 * Partial Implementation For Java9+ support if {@code javax.xml.bind.DatatypeConverter} is missing
 */
@Deprecated
public final class DatatypeConverter {
    private static final SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("%h:%m:%s%z");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("%y-%M-%d%z");

    @Deprecated
    public static int parseInt(String string) {
        return Integer.parseInt(string);
    }

    @Deprecated
    public static byte[] parseBase64Binary(String string) {
        return Base64.getDecoder().decode(string);
    }

    @Deprecated
    public static byte[] parseHexBinary(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i + 1), 16));
        }
        return data;
    }

    @Deprecated
    public static String printInt(int i) {
        return Integer.toString(i);
    }

    @Deprecated
    public static String printUnsignedInt(int i) {
        return Integer.toUnsignedString(i);
    }

    @Deprecated
    public static String printBase64Binary(byte[] binary) {
        return Base64.getEncoder().encodeToString(binary);
    }

    @Deprecated
    public static String printHexBinary(byte[] binary) {
        StringBuilder result = new StringBuilder(binary.length * 2);
        for (byte bb : binary) {
            result.append(String.format("%02X", bb));
        }
        return result.toString();
    }

    @Deprecated
    public static String printTime(java.util.Calendar val) {
        return DatatypeConverter.simpleTimeFormat.format(val.getTime());
    }

    @Deprecated
    public static String printDate(java.util.Calendar val) {
        return DatatypeConverter.simpleDateFormat.format(val.getTime());
    }
}