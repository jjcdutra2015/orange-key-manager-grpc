package br.com.zup.edu.pix.consulta

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.ConsultaChavePixResponse
import br.com.zup.edu.KeyManagerConsultaGrpcServiceGrpc
import br.com.zup.edu.integration.bcb.BcbClient
import br.com.zup.edu.integration.bcb.toModel
import br.com.zup.edu.pix.ChaveRepository
import br.com.zup.edu.pix.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(
    val repository: ChaveRepository,
    val validator: Validator,
    val bcbClient: BcbClient
) : KeyManagerConsultaGrpcServiceGrpc.KeyManagerConsultaGrpcServiceImplBase() {

    override fun consulta(
        request: ConsultaChavePixRequest,
        responseObserver: StreamObserver<ConsultaChavePixResponse>
    ) {

        val filtro = request.toModel(validator)
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient)

        responseObserver.onNext(ConsultaChavePixResponseConverter().convert(chaveInfo))
        responseObserver.onCompleted()
    }
}