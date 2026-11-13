package com.diponegoro.ddb.payment;

import com.diponegoro.ddb.common.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encoder/decoder QRIS mengikuti struktur EMVCo Merchant-Presented QR (TLV + CRC16-CCITT),
 * format standar QRIS Bank Indonesia (disederhanakan untuk simulasi; identik dengan versi Android).
 *
 * Tag: 00 versi, 01 statis(11)/dinamis(12), 26 info akun merchant (00 GUID penerbit, 01 nomor akun,
 * 02 ID merchant, 03 kriteria), 52 MCC, 53 mata uang (360 = IDR), 54 nominal (dinamis), 58 negara,
 * 59 nama, 60 kota, 63 CRC.
 */
public final class QrisCodec {
    public static final String DDB_GUID = "ID.CO.DIPONEGORO.WWW";
    public static final String PERSONAL_MCC = "4829";

    private QrisCodec() {}

    /** amount == null → QRIS statis; terisi → QRIS dinamis (FR-17). */
    public record Payload(String issuerGuid, String accountId, String merchantId, String merchantName, String city,
                          String mcc, Long amount) {
        public boolean dynamic() { return amount != null; }
    }

    public static String encode(Payload p) {
        String account = tlv("00", p.issuerGuid()) + tlv("01", p.accountId()) + tlv("02", p.merchantId()) + tlv("03", "UMI");
        StringBuilder b = new StringBuilder()
                .append(tlv("00", "01"))
                .append(tlv("01", p.dynamic() ? "12" : "11"))
                .append(tlv("26", account))
                .append(tlv("52", p.mcc()))
                .append(tlv("53", "360"));
        if (p.amount() != null) b.append(tlv("54", p.amount().toString()));
        b.append(tlv("58", "ID"))
                .append(tlv("59", cut(p.merchantName(), 25)))
                .append(tlv("60", cut(p.city(), 15)))
                .append("6304");
        String body = b.toString();
        return body + crc16(body);
    }

    /** Melempar ApiException (400) dengan pesan yang bisa ditampilkan bila kode tidak valid. */
    public static Payload decode(String raw) {
        String text = raw == null ? "" : raw.trim();
        require(text.length() > 8 && text.startsWith("6304", text.length() - 8), "Bukan kode QRIS yang valid");
        require(crc16(text.substring(0, text.length() - 4)).equalsIgnoreCase(text.substring(text.length() - 4)),
                "Kode QRIS rusak (CRC tidak cocok)");
        Map<String, String> tags = parse(text.substring(0, text.length() - 8));
        require("01".equals(tags.get("00")), "Versi QRIS tidak didukung");
        require("360".equals(tags.get("53")), "Mata uang QRIS harus Rupiah");
        require("ID".equals(tags.get("58")), "QRIS harus berasal dari Indonesia");
        String accountTag = null;
        for (int t = 26; t <= 51; t++) if (tags.containsKey(String.valueOf(t))) { accountTag = String.valueOf(t); break; }
        require(accountTag != null, "Informasi merchant QRIS tidak ditemukan");
        Map<String, String> account = parse(tags.get(accountTag));
        boolean dynamic = "12".equals(tags.get("01"));
        Long amount = null;
        if (tags.get("54") != null) {
            try { amount = Long.parseLong(tags.get("54").split("\\.")[0]); } catch (NumberFormatException ignored) {}
        }
        require(!dynamic || (amount != null && amount > 0), "QRIS dinamis tidak memiliki nominal");
        require(tags.get("59") != null, "Nama merchant tidak ditemukan");
        return new Payload(account.getOrDefault("00", ""), account.getOrDefault("01", ""), account.getOrDefault("02", ""),
                tags.get("59"), tags.getOrDefault("60", ""), tags.getOrDefault("52", ""), dynamic ? amount : null);
    }

    private static String tlv(String id, String value) {
        if (value.length() > 99) throw new IllegalArgumentException("Nilai tag " + id + " terlalu panjang");
        return id + String.format("%02d", value.length()) + value;
    }

    private static Map<String, String> parse(String data) {
        Map<String, String> result = new LinkedHashMap<>();
        int i = 0;
        while (i < data.length()) {
            require(i + 4 <= data.length(), "Format QRIS tidak valid");
            String id = data.substring(i, i + 2);
            int len;
            try { len = Integer.parseInt(data.substring(i + 2, i + 4)); } catch (NumberFormatException e) { len = -1; }
            require(len >= 0 && i + 4 + len <= data.length(), "Format QRIS tidak valid");
            result.put(id, data.substring(i + 4, i + 4 + len));
            i += 4 + len;
        }
        return result;
    }

    /** CRC16-CCITT (poly 0x1021, init 0xFFFF) sesuai spesifikasi EMV QR. */
    public static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw ApiException.badRequest("INVALID_QRIS", message);
    }
}
