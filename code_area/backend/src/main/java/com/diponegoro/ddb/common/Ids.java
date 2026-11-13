package com.diponegoro.ddb.common;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class Ids {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Ids() {}

    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    /** Deretan digit acak yang aman secara kriptografis (OTP, nomor kartu, token). */
    public static String digits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    public static String randomToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
