package com.diponegoro.digitalbank.authentication.domain.model

data class Credential(
    val credentialId: String,
    val userId: String,
    val passwordHash: String,
    val biometricEnabled: Boolean
)