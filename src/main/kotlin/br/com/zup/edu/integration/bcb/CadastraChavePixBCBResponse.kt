package br.com.zup.edu.integration.bcb

import java.time.LocalDateTime

data class CadastraChavePixBCBResponse(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
)