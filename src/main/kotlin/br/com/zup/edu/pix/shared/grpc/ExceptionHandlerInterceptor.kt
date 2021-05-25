package br.com.zup.edu.pix.shared.grpc

import br.com.zup.edu.pix.registra.RegistraChaveEndpoint
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class)
class ExceptionHandlerInterceptor : MethodInterceptor<RegistraChaveEndpoint, Any?> {

    private val LOGGER = LoggerFactory.getLogger(RegistraChaveEndpoint::class.java)

    override fun intercept(context: MethodInvocationContext<RegistraChaveEndpoint, Any?>): Any? {

        try {
            return context.proceed()
        } catch (e: Exception) {
            val error = when (e) {
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is ChavePixExistenteException -> Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException()
                is ChavePixNaoEncontradaException -> Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
//                is ConstraintViolationException -> Status.INVALID_ARGUMENT.withDescription(e.message)
//                    .asRuntimeException()
                is ConstraintViolationException -> handlerViolationException(e)
                else -> Status.UNKNOWN.withDescription(e.message).asRuntimeException()
            }

            val responseObersever = context.parameterValues[1] as StreamObserver<*>
            responseObersever.onError(error)
            return null
        }
    }

    private fun handlerViolationException(e: ConstraintViolationException): StatusRuntimeException {
        val violations = e.constraintViolations.map {
            BadRequest.FieldViolation.newBuilder()
                .setField(it.propertyPath.last().name)
                .setDescription(it.message)
                .build()
        }

        val details = BadRequest.newBuilder().addAllFieldViolations(violations).build()

        val statusProto = com.google.rpc.Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("Invalid parameters")
            .addDetails(com.google.protobuf.Any.pack(details))
            .build()

        LOGGER.info("Status proto: $statusProto")
        return StatusProto.toStatusRuntimeException(statusProto)
    }
}