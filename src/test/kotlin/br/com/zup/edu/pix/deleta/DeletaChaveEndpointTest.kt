package br.com.zup.edu.pix.deleta

import br.com.zup.edu.DeletaChavePixRequest
import br.com.zup.edu.KeyManagerDeletaGrpcServiceGrpc
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.integration.bcb.BcbClient
import br.com.zup.edu.integration.bcb.DeletaChavePixBCBRequest
import br.com.zup.edu.integration.bcb.DeletaChavePixBCBResponse
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
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class DeletaChaveEndpointTest(
    private val repository: ChaveRepository,
    private val grpcClient: KeyManagerDeletaGrpcServiceGrpc.KeyManagerDeletaGrpcServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BcbClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover uma chave Pix existente`() {
        //cenario
        `when`(bcbClient.deletaChavePix("11111111111", DeletaChavePixBCBRequest("11111111111")))
            .thenReturn(HttpResponse.ok(DeletaChavePixBCBResponse("11111111111", "60701190", LocalDateTime.now())))

        val chavePix = repository.save(
            ChavePix(
                clienteId = CLIENTE_ID,
                tipo = TipoDeChave.CPF,
                chave = "11111111111",
                tipoDeConta = TipoDeConta.CONTA_CORRENTE,
                conta = ContaAssociada(
                    "UNIBANCO ITAU SA", "Rafael Ponte", "11111111111",
                    "1218",
                    "291900"
                )
            )
        )

        //acao
        val response = grpcClient.deleta(
            DeletaChavePixRequest.newBuilder()
                .setClienteId(chavePix.clienteId.toString())
                .setPixId(chavePix.id.toString())
                .build()
        )

        //validacao
        assertEquals(chavePix.id, UUID.fromString(response.pixId))
        assertEquals(chavePix.clienteId, UUID.fromString(response.clienteId))
    }

    @Test
    fun `nao deve remover chave Pix nao encontrada`() {
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(
                DeletaChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setPixId(CLIENTE_ID.toString())
                    .build()
            )
        }

        //validacao
        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave não cadastrada ou não pertence ao cliente", status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix com dados invalidos`() {
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(
                DeletaChavePixRequest.newBuilder().build()
            )
        }

        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Invalid parameters", status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando nao conectar no BCB`() {
        //cenario
        `when`(bcbClient.deletaChavePix("11111111111", DeletaChavePixBCBRequest("11111111111")))
            .thenReturn(HttpResponse.unprocessableEntity())

        val chavePix = repository.save(
            ChavePix(
                clienteId = CLIENTE_ID,
                tipo = TipoDeChave.CPF,
                chave = "11111111111",
                tipoDeConta = TipoDeConta.CONTA_CORRENTE,
                conta = ContaAssociada(
                    "UNIBANCO ITAU SA", "Rafael Ponte", "11111111111", "1218",
                    "291900"
                )
            )
        )

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.deleta(
                DeletaChavePixRequest.newBuilder()
                    .setClienteId(chavePix.clienteId.toString())
                    .setPixId(chavePix.id.toString())
                    .build()
            )
        }

        //validacao
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover chave PIX do Banco Central do Brasil", status.description)
        }
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Client {
        @Bean
        fun clientStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerDeletaGrpcServiceGrpc.KeyManagerDeletaGrpcServiceBlockingStub {
            return KeyManagerDeletaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}