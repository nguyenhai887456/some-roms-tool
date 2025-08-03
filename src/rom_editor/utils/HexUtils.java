package rom_editor.utils;

public class HexUtils {
    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    public static byte[] fromHexString(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static String formatHexString(String hexString) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < hexString.length(); i += 2) {
            if (i > 0) {
                formatted.append(" ");
            }
            formatted.append(hexString.substring(i, Math.min(i + 2, hexString.length())));
        }
        return formatted.toString();
    }
}