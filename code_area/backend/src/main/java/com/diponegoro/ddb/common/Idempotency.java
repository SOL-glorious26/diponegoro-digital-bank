package com.diponegoro.ddb.common;

/**
 * Kunci idempotensi (BR-04). Setiap permintaan yang memindahkan uang wajib mengirim header
 * {@code Idempotency-Key} (mis. UUID yang dibuat klien sekali per transaksi dan dipakai ulang saat retry).
 * Kunci diberi awalan userId agar pengguna berbeda tidak bisa bertabrakan.
 */
public final class Idempotency {
    public static final String HEADER = "Idempotency-Key";
    private Idempotency() {}

    public static String key(String userId, String header) {
        if (header == null || header.isBlank() || header.length() > 64 || !header.matches("[A-Za-z0-9._:-]+")) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED",
                    "Header Idempotency-Key wajib diisi (maks. 64 karakter alfanumerik)");
        }
        return userId + ":" + header;
    }
}
