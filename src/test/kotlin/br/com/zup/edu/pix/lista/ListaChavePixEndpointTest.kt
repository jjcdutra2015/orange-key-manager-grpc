package br.com.zup.edu.pix.lista

import br.com.zup.edu.KeyManagerListaGrpcServiceGrpc
import br.com.zup.edu.ListaChavesPixRequest
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.ContaAssociada
import br.com.zup.edu.pix.TipoDeChave
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChavePixEndpointTest(
    val repository: ChaveRepository,
    val grpcClient: KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceBlockingStub
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.ALEATORIA, chave = "randomkey-2", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = TipoDeChave.ALEATORIA, chave = "randomkey-3", clienteId = CLIENTE_ID))
    }

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar chaves pix do cliente`() {
        //cenario
        val clienteId = CLIENTE_ID.toString()

        //acao
        val response = grpcClient.lista(
            ListaChavesPixRequest.newBuilder()
                .setClienteId(clienteId)
                .build()
        )

        //validacao
        with(response.chavesList) {
            assertThat(this, hasSize(2))
            assertThat(
                this.map { Pair(it.tipoDeChave, it.chave) }.toList(),
                containsInAnyOrder(
                    Pair(br.com.zup.edu.TipoDeChave.ALEATORIA, "randomkey-3"),
                    Pair(br.com.zup.edu.TipoDeChave.EMAIL, "rafael.ponte@zup.com.br")
                )
            )
        }
    }

    @Test
    fun `nao deve listar chaves do cliente quando não possui chave`() {
        //cenario

        //acao
        val response =
            grpcClient.lista(ListaChavesPixRequest.newBuilder().setClienteId(UUID.randomUUID().toString()).build())

        //validacao
        assertEquals(0, response.chavesCount)
    }

    @Test
    fun `nao deve listar chaves quando for nula ou vazia`() {
        //cenario

        //acao
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.lista(ListaChavesPixRequest.newBuilder().setClienteId("").build())
        }

        //validacao
        assertEquals(Status.INVALID_ARGUMENT.code, throws.status.code)
        assertEquals("Chave Pix não pode ser nula ou vazia", throws.status.description)
    }

    @Factory
    class ListClient {
        @Bean
        fun listStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceBlockingStub? {
            return KeyManagerListaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeDoTitular = "Rafael Ponte",
                cpfDoTitular = "12345678900",
                agencia = "1218",
                numeroDaConta = "123456"
            )
        )
    }
}