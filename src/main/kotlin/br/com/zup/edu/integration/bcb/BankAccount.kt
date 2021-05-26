package br.com.zup.edu.integration.bcb

import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.pix.ContaAssociada

data class BankAccount(
    val participant: String = ContaAssociada.ITAU_UNIBANCO_ISPB,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
) {
    enum class AccountType {
        CACC,
        SVGS;

        companion object {
            fun by(domainType: TipoDeConta): AccountType {
                return when (domainType) {
                    TipoDeConta.CONTA_CORRENTE -> CACC
                    TipoDeConta.CONTA_POUPANCA -> SVGS
                    TipoDeConta.UNKNOWN_CONTA -> TODO()
                    TipoDeConta.UNRECOGNIZED -> TODO()
                }
            }
        }
    }
}
