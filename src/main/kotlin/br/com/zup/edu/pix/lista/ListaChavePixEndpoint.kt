package br.com.zup.edu.pix.lista

import br.com.zup.edu.KeyManagerListaGrpcServiceGrpc
import br.com.zup.edu.ListaChavesPixRequest
import br.com.zup.edu.ListaChavesPixResponse
import br.com.zup.edu.TipoDeChave
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.time.ZoneId
import java.util.*
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChavePixEndpoint(val repository: ChaveRepository) :
    KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceImplBase() {

    override fun lista(request: ListaChavesPixRequest, responseObserver: StreamObserver<ListaChavesPixResponse>) {

        if (request.clienteId.isNullOrBlank()) {
            throw IllegalArgumentException("Chave Pix não pode ser nula ou vazia")
        }

        val listaChavePix = repository.findAllByClienteId(UUID.fromString(request.clienteId))

        val chaves = listaChavePix.map {
            ListaChavesPixResponse.ChavesPix.newBuilder()
                .setPixId(it.id.toString())
                .setClienteId(it.clienteId.toString())
                .setTipoDeChave(TipoDeChave.valueOf(it.tipo.name))
                .setChave(it.chave)
                .setTipoDeConta(it.tipoDeConta)
                .setRegistradaEm(it.criadaEm.let {
                    val instant = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder().setSeconds(instant.epochSecond).setNanos(instant.nano).build()
                })
                .build()
        }

        responseObserver.onNext(
            ListaChavesPixResponse.newBuilder()
                .addAllChaves(chaves)
                .build()
        )

        responseObserver.onCompleted()
    }
}