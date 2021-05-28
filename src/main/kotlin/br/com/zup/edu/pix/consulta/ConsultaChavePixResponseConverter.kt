package br.com.zup.edu.pix.consulta

import br.com.zup.edu.ConsultaChavePixResponse
import br.com.zup.edu.TipoDeConta
import br.com.zup.edu.integration.bcb.ChavePixInfo
import br.com.zup.edu.pix.TipoDeChave
import com.google.protobuf.Timestamp
import java.time.ZoneId

class ConsultaChavePixResponseConverter {

    fun convert(chaveInfo: ChavePixInfo): ConsultaChavePixResponse {
        return ConsultaChavePixResponse.newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setPixId(chaveInfo.pixId?.toString() ?: "")
            .setChave(
                ConsultaChavePixResponse.ChavePix.newBuilder()
                    .setTipoDeChave(TipoDeChave.valueOf(chaveInfo.tipo.name).toString())
                    .setChave(chaveInfo.chave)
                    .setInfo(
                        ConsultaChavePixResponse.ChavePix.ContaInfo.newBuilder()
                            .setTipoDeConta(TipoDeConta.valueOf(chaveInfo.tipoDeConta.name))
                            .setInstituicao(chaveInfo.conta.instituicao)
                            .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                            .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                            .setAgencia(chaveInfo.conta.agencia)
                            .setNumero(chaveInfo.conta.numeroDaConta)
                            .build()
                    )
                    .setCriadaEm(chaveInfo.registradaEm.let {
                        val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                        Timestamp.newBuilder()
                            .setSeconds(createdAt.epochSecond)
                            .setNanos(createdAt.nano)
                            .build()
                    })
                    .build()
            )
            .build()
    }
}
