package com.diponegoro.ddb.common;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private static final Locale ID = Locale.of("id", "ID");
    private Money() {}

    /** 1500000 → "Rp1.500.000" (sama dengan format di aplikasi Android). */
    public static String rupiah(long amount) {
        NumberFormat f = NumberFormat.getIntegerInstance(ID);
        return (amount < 0 ? "-Rp" : "Rp") + f.format(Math.abs(amount));
    }
}
