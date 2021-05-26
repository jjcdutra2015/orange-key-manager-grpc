package br.com.zup.edu.pix.deleta

import br.com.zup.edu.integration.bcb.BcbClient
import br.com.zup.edu.integration.bcb.DeletaChavePixBCBRequest
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.ValidUUID
import br.com.zup.edu.pix.shared.grpc.ChavePixNaoEncontradaException
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Singleton
@Validated
class DeletaChavePixService(val repository: ChaveRepository, val bcbClient: BcbClient) {

    @Transactional
    fun deleta(
        @NotBlank @ValidUUID(message = "Cliente ID com formato inválido") clienteId: String?,
        @NotBlank @ValidUUID(message = "PixID com formato inválido") pixId: String?
    ) {

        val uuidClienteId = UUID.fromString(clienteId)
        val uuidPixId = UUID.fromString(pixId)

        val chavePix = repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow { ChavePixNaoEncontradaException("Chave não cadastrada ou não pertence ao cliente") }

        repository.deleteById(uuidPixId)

        val deletaChavePixBCBRequest = DeletaChavePixBCBRequest.of(chavePix!!)

        val deletaChavePixResponse = bcbClient.deletaChavePix(chavePix.chave, deletaChavePixBCBRequest)
        if (deletaChavePixResponse.status() != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover chave PIX do Banco Central do Brasil")
        }
    }
}