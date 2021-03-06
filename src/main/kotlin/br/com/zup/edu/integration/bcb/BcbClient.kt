package br.com.zup.edu.integration.bcb

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.pix.url}")
interface BcbClient {

    @Post(
        value = "/api/v1/pix/keys",
        produces = [MediaType.APPLICATION_XML],
        consumes = [MediaType.APPLICATION_XML]
    )
    fun cadastraChavePix(@Body cadastraChavePixBCBRequest: CadastraChavePixBCBRequest): HttpResponse<CadastraChavePixBCBResponse>

    @Delete(
        value = "/api/v1/pix/keys/{key}",
        produces = [MediaType.APPLICATION_XML],
        consumes = [MediaType.APPLICATION_XML]
    )
    fun deletaChavePix(
        @PathVariable key: String,
        @Body deletaChavePixBCBRequest: DeletaChavePixBCBRequest
    ): HttpResponse<DeletaChavePixBCBResponse>

    @Get(
        value = "/api/v1/pix/keys/{key}",
        consumes = [MediaType.APPLICATION_XML]
    )
    fun consultaChavePix(@PathVariable key: String): HttpResponse<DetalhesChavePixResponse>
}