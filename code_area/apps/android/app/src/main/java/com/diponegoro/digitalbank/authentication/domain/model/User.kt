package com.diponegoro.digitalbank.authentication.domain.model

data class User(
    val userId: String,
    val email: String,
    val phoneNumber: String,
    val username: String,
    val accountStatus: AccountStatus,
    /** URI foto profil lokal (FR-27). */
    val photoUri: String? = null
)