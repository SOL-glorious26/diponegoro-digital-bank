package com.diponegoro.digitalbank.core.storage

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/** Penyimpanan preferensi kecil (mis. pilihan sembunyikan saldo, FR-06). */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences("ddb_prefs", Context.MODE_PRIVATE)
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

class InMemoryStore : KeyValueStore {
    private val values = ConcurrentHashMap<String, Boolean>()
    override fun getBoolean(key: String, default: Boolean) = values[key] ?: default
    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
