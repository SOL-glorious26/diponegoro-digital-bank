package com.diponegoro.digitalbank.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Semua nominal uang disimpan sebagai Long (rupiah penuh) untuk menghindari kesalahan pembulatan. */
fun formatRupiah(amount: Long): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return (if (negative) "-Rp" else "Rp") + grouped
}

/** Mengambil angka dari input bebas ("Rp 50.000" → 50000). Null bila kosong/terlalu besar. */
fun parseRupiah(raw: String): Long? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty() || digits.length > 15) return null
    return digits.toLongOrNull()
}

val ID_LOCALE: Locale = Locale.forLanguageTag("id-ID")

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", ID_LOCALE).format(Date(millis))

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", ID_LOCALE).format(Date(millis))

/** Nomor rekening dikelompokkan 4 digit agar mudah dibaca. */
fun formatAccountNumber(number: String): String = number.chunked(4).joinToString(" ")
