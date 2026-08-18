package no.nav.bidrag.grunnlag.consumer.familiebasak

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.grunnlag.consumer.GrunnlagsConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod

private const val FAMILIEBASAK_CONTEXT = "/api/bisys/hent-utvidet-barnetrygd"

open class FamilieBaSakConsumer(private val restTemplate: HttpHeaderRestTemplate) : GrunnlagsConsumer() {

    companion object {
        @JvmStatic
        val logger: Logger = LoggerFactory.getLogger(FamilieBaSakConsumer::class.java)
    }

    open fun hentFamilieBaSak(request: FamilieBaSakRequest): RestResponse<FamilieBaSakResponse> {
        logger.debug("Henter utvidet barnetrygd og småbarnstillegg fra familie-ba-sak")

        val restResponse = restTemplate.tryExchange(
            FAMILIEBASAK_CONTEXT,
            HttpMethod.POST,
            initHttpEntity(request),
            FamilieBaSakResponse::class.java,
            FamilieBaSakResponse(emptyList()),
        )

        logResponse("Utvidet barnetrygd og småbarnstillegg", request.personIdent, request.fraDato, null, restResponse)

        return restResponse
    }
}
