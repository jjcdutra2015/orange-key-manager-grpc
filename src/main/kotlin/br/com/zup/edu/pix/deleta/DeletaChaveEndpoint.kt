package br.com.zup.edu.pix.deleta

import br.com.zup.edu.DeletaChavePixRequest
import br.com.zup.edu.DeletaChavePixResponse
import br.com.zup.edu.KeyManagerDeletaGrpcServiceGrpc
import br.com.zup.edu.pix.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Singleton

@ErrorHandler
@Singleton
class DeletaChaveEndpoint(
    val service: DeletaChavePixService
) : KeyManagerDeletaGrpcServiceGrpc.KeyManagerDeletaGrpcServiceImplBase() {

    override fun deleta(
        request: DeletaChavePixRequest,
        responseObserver: StreamObserver<DeletaChavePixResponse>
    ) {

        service.deleta(clienteId = request.clienteId, pixId = request.pixId)

        responseObserver.onNext(
            DeletaChavePixResponse.newBuilder()
                .setClienteId(request.clienteId)
                .setPixId(request.pixId)
                .build()
        )

        responseObserver.onCompleted()
    }
}