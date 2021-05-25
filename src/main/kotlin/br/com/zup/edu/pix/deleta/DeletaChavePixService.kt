package br.com.zup.edu.pix.deleta

import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.ValidUUID
import br.com.zup.edu.pix.shared.grpc.ChavePixNaoEncontradaException
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Singleton
@Validated
class DeletaChavePixService(val repository: ChaveRepository) {

    @Transactional
    fun deleta(
        @NotBlank @ValidUUID(message = "Cliente ID com formato inválido") clienteId: String?,
        @NotBlank @ValidUUID(message = "PixID com formato inválido") pixId: String?
    ) {

        val uuidClienteId = UUID.fromString(clienteId)
        val uuidPixId = UUID.fromString(pixId)

        repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow { ChavePixNaoEncontradaException("Chave não cadastrada ou não pertence ao cliente") }

        repository.deleteById(uuidPixId)
    }
}