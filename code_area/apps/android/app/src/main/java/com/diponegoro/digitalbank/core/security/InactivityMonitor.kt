package com.diponegoro.digitalbank.core.security

/** FR-05: mencatat interaksi terakhir pengguna untuk timeout sesi otomatis. */
object InactivityMonitor {
    /** Batas tidak aktif sebelum sesi diakhiri. */
    const val TIMEOUT_MS = 5 * 60 * 1000L

    @Volatile
    var lastInteraction: Long = System.currentTimeMillis()
        private set

    fun touch() {
        lastInteraction = System.currentTimeMillis()
    }

    fun isExpired(now: Long = System.currentTimeMillis()) = now - lastInteraction >= TIMEOUT_MS
}
