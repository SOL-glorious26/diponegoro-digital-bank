package com.diponegoro.ddb.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Jam yang bisa dimajukan dari test (mis. menunggu e-KYC diputuskan, PIN terbuka blokirnya). */
public class MutableClock extends Clock {
    private volatile Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration d) {
        now = now.plus(d);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    /** Berdetak 1 ms setiap dibaca, agar urutan kejadian dalam satu test tetap jelas (seperti jam sungguhan). */
    @Override public synchronized Instant instant() {
        now = now.plusMillis(1);
        return now;
    }
}
