package br.com.zup.edu.integration.bcb

import br.com.zup.edu.integration.bcb.BankAccount.AccountType
import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ContaAssociada

data class CadastraChavePixBCBRequest(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner
) {
    companion object {
        fun of(chave: ChavePix): CadastraChavePixBCBRequest {
            return CadastraChavePixBCBRequest(
                keyType = PixKeyType.by(chave.tipo),
                key = chave.chave,
                bankAccount = BankAccount(
                    participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                    branch = chave.conta.agencia,
                    accountNumber = chave.conta.numeroDaConta,
                    accountType = AccountType.by(chave.tipoDeConta)
                ),
                owner = Owner(
                    type = Owner.OwnerType.NATURAL_PERSON,
                    name = chave.conta.nomeDoTitular,
                    taxIdNumber = chave.conta.cpfDoTitular
                )
            )
        }
    }
}