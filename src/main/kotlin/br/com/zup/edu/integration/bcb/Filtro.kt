package br.com.zup.edu.integration.bcb

import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.ValidUUID
import br.com.zup.edu.pix.shared.grpc.ChavePixNaoEncontradaException
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    /**
     * Deve retornar chave encontrada ou lançar uma excessão de chave não encontrada
     */
    abstract fun filtra(repository: ChaveRepository, bcbClient: BcbClient): ChavePixInfo

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String
    ) : Filtro() {
        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChaveRepository, bcbClient: BcbClient): ChavePixInfo {
            return repository.findById(pixIdAsUuid())
                .filter { it.pertenceAo(clienteIdAsUuid()) }
                .map(ChavePixInfo::of)
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
        }
    }

    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() {
        override fun filtra(repository: ChaveRepository, bcbClient: BcbClient): ChavePixInfo {
            return repository.findByChave(chave)
                .map(ChavePixInfo::of)
                .orElseGet {
                    val response = bcbClient.consultaChavePix(chave)
                    when (response.status) {
                        HttpStatus.OK -> response.body()?.toModel()
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
                    }
                }
        }
    }

    @Introspected
    class Invalido() : Filtro() {
        override fun filtra(repository: ChaveRepository, bcbClient: BcbClient): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }
}
