package br.com.zup.edu.pix.registra

import br.com.zup.edu.integration.bcb.BcbClient
import br.com.zup.edu.integration.bcb.CadastraChavePixBCBRequest
import br.com.zup.edu.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.pix.ChavePix
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.shared.grpc.ChavePixExistenteException
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
    @Inject val repository: ChaveRepository,
    @Inject val itauClient: ContasDeClientesNoItauClient,
    @Inject val bcbClient: BcbClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1.Verifica se chave existe no sistema
        if (repository.existsByChave(novaChave.chave)) {
            throw ChavePixExistenteException("Chave pix '${novaChave.chave}' existente")
        }

        //2.Busca dados da conta no ERP do Itau
        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no itau")

        //3.Grava no banco de dados
        val chave = novaChave.toModel(conta)

        repository.save(chave)

        val chavePixBCBRequest = CadastraChavePixBCBRequest.of(chave)

        val cadastraChavePixResponse = bcbClient.cadastraChavePix(chavePixBCBRequest)
        if (cadastraChavePixResponse.status != HttpStatus.CREATED) {
            throw IllegalStateException("Erro ao cadastrar chave PIX no Banco Central do Brasil")
        }

        chave.atualizaPixBCB(cadastraChavePixResponse.body().key)

        return chave
    }
}