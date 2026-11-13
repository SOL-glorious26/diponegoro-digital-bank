package com.diponegoro.digitalbank.core.util

/** Hasil operasi bersama untuk modul Account, Transfer, Payment, Top-up, History, dan Profile. */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Failure(val message: String) : AppResult<Nothing>()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onFailure(block: (String) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) block(message)
    return this
}
