package br.com.zup.edu.pix.registra

import br.com.zup.edu.ChavePixRequest
import br.com.zup.edu.KeyManagerGrpcServiceGrpc
import br.com.zup.edu.TipoDeChave
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.integration.itau.DadosDaContaResponse
import br.com.zup.edu.integration.itau.InstituicaoResponse
import br.com.zup.edu.integration.itau.TitularResponse
import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.ContaAssociada
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
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    private val repository: ChaveRepository,
    private val grpcClient: KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ContasDeClientesNoItauClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {
        //cenario
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        //acao
        val chavePixResponse = grpcClient.cadastra(
            ChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoDeChave(TipoDeChave.EMAIL)
                .setChave("teste@zup.com.br")
                .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                .build()
        )

        //validacao
        with(chavePixResponse) {
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve cadastrar chave pix duplicada`() {
        //cenario
        repository.save(
            ChavePix(
                clienteId = CLIENTE_ID,
                tipo = br.com.zup.edu.pix.TipoDeChave.CPF,
                chave = "11111111111",
                tipoDeConta = TipoDeConta.CONTA_CORRENTE,
                conta = ContaAssociada(
                    "UNIBANCO ITAU SA",
                    "Rafael Ponte",
                    "11111111111",
                    "1218",
                    "291900"
                )
            )
        )

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                ChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoDeChave(TipoDeChave.CPF)
                    .setChave("11111111111")
                    .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                    .build()
            )
        }

        //validacao
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave pix '11111111111' existente", status.description)
        }
    }

    @Test
    fun `nao deve cadastrar chave pix quando nao encontra cliente na base do ERP do Itau`() {
        //cenárop
        `when`(itauClient.buscaContaPorTipo(CLIENTE_ID.toString(), "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        //ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                ChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoDeChave(TipoDeChave.EMAIL)
                    .setChave("teste@zup.com.br")
                    .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                    .build()
            )
        }

        //validação
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no itau", status.description)
        }
    }

    @Test
    fun `nao deve cadastrar chave pix com parametros invalidos`() {
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(ChavePixRequest.newBuilder().build())
        }

        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Invalid parameters", status.description)
        }
    }

    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient(): ContasDeClientesNoItauClient? {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    @Factory
    class ChaveClient {
        @Bean
        fun chaveStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceBlockingStub? {
            return KeyManagerGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", "60701190"),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafael pont", "11111111111")
        )
    }

}