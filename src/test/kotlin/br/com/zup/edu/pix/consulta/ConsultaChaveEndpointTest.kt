package br.com.zup.edu.pix.consulta

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.KeyManagerConsultaGrpcServiceGrpc
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.integration.bcb.*
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest(
    val repository: ChaveRepository,
    val grpcClient: KeyManagerConsultaGrpcServiceGrpc.KeyManagerConsultaGrpcServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BcbClient

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve consultar uma chave passando o pixId e o clienteId`() {
        //cenario
        val chavePix = repository.save(
            ChavePix(
                clienteId = UUID.randomUUID(),
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

        val possivelChave = repository.findByChave(chavePix.chave).get()

        //acao
        val chaveResponse = grpcClient.consulta(
            ConsultaChavePixRequest.newBuilder()
                .setPixId(
                    ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                        .setClienteId(possivelChave.clienteId.toString())
                        .setPixId(possivelChave.id.toString())
                        .build()
                )
                .build()
        )

        //validacao
        with(chaveResponse) {
            assertEquals(possivelChave.clienteId.toString(), clienteId)
            assertEquals(possivelChave.id.toString(), pixId)
            assertEquals(possivelChave.chave, chave.chave)
            assertEquals(possivelChave.tipo.name, chave.tipoDeChave)
        }
    }

    @Test
    fun `nao deve carregar uma chave com pixId e clienteId invalido`() {
        //acao
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                ConsultaChavePixRequest.newBuilder()
                    .setPixId(
                        ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setPixId("")
                            .setClienteId("")
                            .build()
                    ).build()
            )
        }

        //validacao
        with(throws) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
//            assertEquals("Invalid parameters", status.description)
        }

    }

    @Test
    fun `nao deve carregar uma chave quando o pixId e clienteId nao existir`() {
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                ConsultaChavePixRequest.newBuilder()
                    .setPixId(
                        ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setPixId(UUID.randomUUID().toString())
                            .setClienteId(UUID.randomUUID().toString())
                            .build()
                    )
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave quando passado uma chave existente localmente`() {
        //cenario
        val chavePix = repository.save(
            ChavePix(
                clienteId = UUID.randomUUID(),
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
        val possivelChave = repository.findByChave(chavePix.chave).get()

        //validacao
        assertEquals(possivelChave.id, chavePix.id)
        assertEquals(possivelChave.clienteId, chavePix.clienteId)
        assertEquals(possivelChave.conta.nomeDoTitular, chavePix.conta.nomeDoTitular)
        assertEquals(possivelChave.chave, chavePix.chave)
    }

    @Test
    fun `deve carregar chave quando passado uma chave nao existente localmente e existente no BACEN`() {
        //cenario
        val bcbResponse = detalhesChavePixResponse()
        `when`(bcbClient.consultaChavePix(key = "teste@mail.com.br"))
            .thenReturn(HttpResponse.ok(detalhesChavePixResponse()))

        //acao
        val response = grpcClient.consulta(ConsultaChavePixRequest.newBuilder().setChave("teste@mail.com.br").build())

        //validacao
        assertEquals("", response.pixId)
        assertEquals("", response.clienteId)
        assertEquals(bcbResponse.keyType.name, response.chave.tipoDeChave)
        assertEquals(bcbResponse.key, response.chave.chave)
    }

    @Test
    fun `deve carregar chave quando passado uma chave nao existente localmente e nao existente no BACEN`() {
        //cenario
        `when`(bcbClient.consultaChavePix(key = "teste@mail.com.br")).thenReturn(HttpResponse.notFound())

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder().setChave("teste@mail.com.br").build())
        }

        //validacao
        assertEquals(Status.NOT_FOUND.code, error.status.code)
        assertEquals("Chave Pix não encontrada", error.status.description)
    }

    @Test
    fun `nao deve carregar chave com valor invalido da chave no filtro`() {
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder().setChave("").build())
        }

        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Invalid parameters", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave com valores invalidos`() {
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder().build())
        }

        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Client() {
        @Bean
        fun clientStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerConsultaGrpcServiceGrpc.KeyManagerConsultaGrpcServiceBlockingStub? {
            return KeyManagerConsultaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun detalhesChavePixResponse(): DetalhesChavePixResponse {
        return DetalhesChavePixResponse(
            keyType = PixKeyType.EMAIL,
            key = "teste@mail.com.br",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = "90400888",
            branch = "9871",
            accountNumber = "987654",
            accountType = BankAccount.AccountType.SVGS
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Another User",
            taxIdNumber = "12345678901"
        )
    }
}