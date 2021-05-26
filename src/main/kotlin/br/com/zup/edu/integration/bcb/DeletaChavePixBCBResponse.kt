package br.com.zup.edu.integration.bcb

import java.time.LocalDateTime

data class DeletaChavePixBCBResponse(
    val key: String,
    val participant: String,
    val deletedAt: LocalDateTime
)
