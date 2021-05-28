package br.com.zup.edu.integration.bcb

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.ConsultaChavePixRequest.FiltroCase.*
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ConsultaChavePixRequest.toModel(validator: Validator) : Filtro {

    val filtro = when(filtroCase) {
        PIXID -> pixId.let {
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId)
        }
        CHAVE -> Filtro.PorChave(chave)
        FILTRO_NOT_SET -> Filtro.Invalido()
    }

    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }

    return filtro
}