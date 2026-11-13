package com.diponegoro.digitalbank.payment.domain

/**
 * Encoder/decoder QRIS mengikuti struktur EMVCo Merchant-Presented QR (TLV + CRC16-CCITT),
 * format yang dipakai standar QRIS Bank Indonesia. Disederhanakan untuk simulasi.
 *
 * Tag yang dipakai: 00 versi, 01 statis(11)/dinamis(12), 26 info akun merchant
 * (00 GUID penerbit, 01 nomor akun, 02 ID merchant, 03 kriteria), 52 MCC, 53 mata uang (360 = IDR),
 * 54 nominal (hanya QRIS dinamis), 58 negara, 59 nama, 60 kota, 63 CRC.
 */
data class QrisPayload(
    val issuerGuid: String,
    val accountId: String,
    val merchantId: String,
    val merchantName: String,
    val city: String,
    val mcc: String,
    /** Null untuk QRIS statis (pembayar mengisi nominal); terisi untuk QRIS dinamis (FR-17). */
    val amount: Long?
) {
    val isDynamic: Boolean get() = amount != null
}

object QrisCodec {
    const val DDB_GUID = "ID.CO.DIPONEGORO.WWW"
    const val PERSONAL_MCC = "4829" // transfer dana / perorangan

    fun encode(payload: QrisPayload): String {
        val account = tlv("00", payload.issuerGuid) +
            tlv("01", payload.accountId) +
            tlv("02", payload.merchantId) +
            tlv("03", "UMI")
        val body = buildString {
            append(tlv("00", "01"))
            append(tlv("01", if (payload.isDynamic) "12" else "11"))
            append(tlv("26", account))
            append(tlv("52", payload.mcc))
            append(tlv("53", "360"))
            payload.amount?.let { append(tlv("54", it.toString())) }
            append(tlv("58", "ID"))
            append(tlv("59", payload.merchantName.take(25)))
            append(tlv("60", payload.city.take(15)))
            append("6304")
        }
        return body + crc16(body)
    }

    fun decode(raw: String): Result<QrisPayload> = runCatching {
        val text = raw.trim()
        require(text.length > 8 && text.substring(text.length - 8, text.length - 4) == "6304") {
            "Bukan kode QRIS yang valid"
        }
        val expectedCrc = crc16(text.dropLast(4))
        require(expectedCrc.equals(text.takeLast(4), ignoreCase = true)) { "Kode QRIS rusak (CRC tidak cocok)" }

        val tags = parse(text.dropLast(8))
        require(tags["00"] == "01") { "Versi QRIS tidak didukung" }
        require(tags["53"] == "360") { "Mata uang QRIS harus Rupiah" }
        require(tags["58"] == "ID") { "QRIS harus berasal dari Indonesia" }
        val accountTag = (26..51).map { it.toString() }.firstOrNull { tags.containsKey(it) }
            ?: error("Informasi merchant QRIS tidak ditemukan")
        val account = parse(tags.getValue(accountTag))
        val dynamic = tags["01"] == "12"
        val amount = tags["54"]?.substringBefore('.')?.toLongOrNull()
        require(!dynamic || (amount != null && amount > 0)) { "QRIS dinamis tidak memiliki nominal" }

        QrisPayload(
            issuerGuid = account["00"].orEmpty(),
            accountId = account["01"].orEmpty(),
            merchantId = account["02"].orEmpty(),
            merchantName = tags["59"] ?: error("Nama merchant tidak ditemukan"),
            city = tags["60"].orEmpty(),
            mcc = tags["52"].orEmpty(),
            amount = if (dynamic) amount else null
        )
    }

    private fun tlv(id: String, value: String): String {
        require(value.length <= 99) { "Nilai tag $id terlalu panjang" }
        return id + value.length.toString().padStart(2, '0') + value
    }

    private fun parse(data: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var i = 0
        while (i < data.length) {
            require(i + 4 <= data.length) { "Format QRIS tidak valid" }
            val id = data.substring(i, i + 2)
            val len = data.substring(i + 2, i + 4).toIntOrNull() ?: error("Format QRIS tidak valid")
            require(i + 4 + len <= data.length) { "Format QRIS tidak valid" }
            result[id] = data.substring(i + 4, i + 4 + len)
            i += 4 + len
        }
        return result
    }

    /** CRC16-CCITT (poly 0x1021, init 0xFFFF) sesuai spesifikasi EMV QR. */
    fun crc16(data: String): String {
        var crc = 0xFFFF
        for (byte in data.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }
}
