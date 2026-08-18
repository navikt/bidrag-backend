package no.nav.bidrag.grunnlag.consumer.bidragperson

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.api.HusstandsmedlemmerRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragPersonConsumer(
    @Value("\${BIDRAG_PERSON_URL}") bidragPersonUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "bidrag-person") {

    private val hentNavnFødselDødUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("navnfoedseldoed")
            .build()
            .toUriString()

    private val hentForelderBarnRelasjonUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("forelderbarnrelasjon")
            .build()
            .toUriString()

    private val hentHusstandsmedlemmerUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("husstandsmedlemmer")
            .build()
            .toUriString()

    private val hentSivilstandUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("sivilstand")
            .build()
            .toUriString()

    private val hentPersonidenterUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("personidenter")
            .build()
            .toUriString()

    fun hentNavnFødselOgDød(personident: Personident): RestResponse<NavnFødselDødDto> {
        val restResponse = restTemplate.tryExchange(
            url = hentNavnFødselDødUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(PersonRequest(personident)),
            responseType = NavnFødselDødDto::class.java,
            fallbackBody = NavnFødselDødDto(navn = "", fødselsdato = null, fødselsår = 0, dødsdato = null),
        )

        grunnlagConsumer.logResponse(
            type = "Navn, fødsel og død",
            ident = personident.toString(),
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }

    fun hentForelderBarnRelasjon(personident: Personident): RestResponse<ForelderBarnRelasjonDto> {
        val restResponse = restTemplate.tryExchange(
            url = hentForelderBarnRelasjonUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(PersonRequest(personident)),
            responseType = ForelderBarnRelasjonDto::class.java,
            fallbackBody = ForelderBarnRelasjonDto(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Forelder-barn-relasjon",
            ident = personident.toString(),
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }

    fun hentHusstandsmedlemmer(request: HusstandsmedlemmerRequest): RestResponse<HusstandsmedlemmerDto> {
        val restResponse = restTemplate.tryExchange(
            url = hentHusstandsmedlemmerUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = HusstandsmedlemmerDto::class.java,
            fallbackBody = HusstandsmedlemmerDto(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Husstandsmedlemmer",
            ident = request.personRequest.ident.toString(),
            fom = request.periodeFra,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }

    fun hentSivilstand(personident: Personident): RestResponse<SivilstandPdlHistorikkDto> {
        val restResponse = restTemplate.tryExchange(
            url = hentSivilstandUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(PersonRequest(personident)),
            responseType = SivilstandPdlHistorikkDto::class.java,
            fallbackBody = SivilstandPdlHistorikkDto(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Sivilstand",
            ident = personident.toString(),
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }

    fun hentPersonidenter(personident: Personident, inkludereHistoriske: Boolean): RestResponse<List<PersonidentDto>> {
        val responseType = object : ParameterizedTypeReference<List<PersonidentDto>>() {}

        val restResponse = restTemplate.tryExchange(
            url = hentPersonidenterUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(
                HentePersonidenterRequest(
                    ident = personident.verdi,
                    grupper = setOf(Identgruppe.FOLKEREGISTERIDENT),
                    inkludereHistoriske = inkludereHistoriske,
                ),
            ),
            responseType = responseType,
            fallbackBody = emptyList(),
        )

        grunnlagConsumer.logResponse(
            type = "Personidenter",
            ident = personident.toString(),
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
