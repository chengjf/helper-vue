package de.jonashackt.springbootvuejs.utils;

import java.security.MessageDigest;

public class MD5Util {
    public static String encode32(String message) {
        byte[] unencodedPassword = message.getBytes();
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception var6) {
            return message;
        }

        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < encodedPassword.length; ++i) {
            if ((encodedPassword[i] & 255) < 16) {
                buf.append("0");
            }

            buf.append(Long.toString((long) (encodedPassword[i] & 255), 16));
        }

        return buf.toString();
    }

    public static String encode16(String message) {
        return encode32(message).substring(8, 24);
    }
}
