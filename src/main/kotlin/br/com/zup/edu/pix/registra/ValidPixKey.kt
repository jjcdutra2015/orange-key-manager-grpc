package br.com.zup.edu.pix

import br.com.zup.edu.pix.registra.NovaChavePix
import javax.inject.Singleton
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@MustBeDocumented
@Target(CLASS, TYPE, VALUE_PARAMETER)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidPixKeyValidator::class])
annotation class ValidPixKey(
    val message: String = "Chave Pix inválida (\${validatedValue.tipo})",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = []
)

@Singleton
class ValidPixKeyValidator : ConstraintValidator<ValidPixKey, NovaChavePix> {
    override fun isValid(
        value: NovaChavePix?,
        context: ConstraintValidatorContext?
    ): Boolean {
        if (value?.tipo == null) {
            return false
        }

        return value.tipo.valida(value.chave)
    }

}
