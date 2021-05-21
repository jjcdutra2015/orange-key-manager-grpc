package br.com.zup.edu.pix.registra

import br.com.zup.edu.ChavePixRequest
import br.com.zup.edu.TipoDeChave.UNKNOWN_CHAVE
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.TipoDeConta.UNKNOWN_CONTA
import br.com.zup.edu.pix.TipoDeChave

fun ChavePixRequest.toModel(): NovaChavePix {
    return NovaChavePix(
        clienteId = clienteId,
        tipo = when (tipoDeChave) {
            UNKNOWN_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}