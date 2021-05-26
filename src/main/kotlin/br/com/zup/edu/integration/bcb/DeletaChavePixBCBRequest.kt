package br.com.zup.edu.integration.bcb

import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ContaAssociada

data class DeletaChavePixBCBRequest(
    val key: String,
    val participant: String
) {
    companion object {
        fun of(chavePix: ChavePix): DeletaChavePixBCBRequest {
            return DeletaChavePixBCBRequest(
                key = chavePix!!.chave,
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB
            )
        }
    }
}
