package com.diponegoro.digitalbank.authentication.domain.model

data class EkycVerification(
    val ekycId: String,
    val userId: String,
    val ktpDataReference: String,
    val selfieReference: String,
    val status: EkycStatus
)