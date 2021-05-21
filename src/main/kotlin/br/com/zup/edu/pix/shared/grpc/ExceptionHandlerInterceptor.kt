package br.com.zup.edu.pix.shared.grpc

import br.com.zup.edu.pix.registra.ChavePixExistenteException
import br.com.zup.edu.pix.registra.RegistraChaveEndpoint
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class)
class ExceptionHandlerInterceptor : MethodInterceptor<RegistraChaveEndpoint, Any?> {

    override fun intercept(context: MethodInvocationContext<RegistraChaveEndpoint, Any?>): Any? {

        try {
            return context.proceed()
        } catch (e: Exception) {
            val error = when (e) {
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is ChavePixExistenteException -> Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException()
                is ConstraintViolationException -> Status.INVALID_ARGUMENT.withDescription(e.message)
                    .asRuntimeException()
                else -> Status.UNKNOWN.withDescription(e.message).asRuntimeException()
            }

            val responseObersever = context.parameterValues[1] as StreamObserver<*>
            responseObersever.onError(error)
            return null
        }
    }
}