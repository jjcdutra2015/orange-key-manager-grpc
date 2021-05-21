package br.com.zup.edu.pix.registra

import br.com.zup.edu.ChavePixRequest
import br.com.zup.edu.ChavePixResponse
import br.com.zup.edu.KeyManagerGrpcServiceGrpc
import br.com.zup.edu.pix.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import io.micronaut.validation.Validated
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Validated
@Singleton
class RegistraChaveEndpoint(@Inject private val service: NovaChavePixService) :
    KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceImplBase() {

    override fun cadastra(
        request: ChavePixRequest,
        responseObserver: StreamObserver<ChavePixResponse>
    ) {

        val novaChave = request.toModel()
        val chaveCriada = service.registra(novaChave)

        responseObserver.onNext(
            ChavePixResponse.newBuilder()
                .setClienteId(chaveCriada.clienteId.toString())
                .setPixId(chaveCriada.id.toString()).build()
        )
        responseObserver.onCompleted()
    }
}
