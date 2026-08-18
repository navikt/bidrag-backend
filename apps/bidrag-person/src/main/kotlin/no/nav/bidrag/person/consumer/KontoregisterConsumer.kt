package no.nav.bidrag.person.consumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.person.dto.KontoregisterRequest
import no.nav.bidrag.person.dto.KontoregisterResponse
import no.nav.bidrag.transport.person.KontonummerDto
import no.nav.bidrag.transport.person.MetadataDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.time.LocalDateTime

@Component
class KontoregisterConsumer(@param:Qualifier("kontoregister") private val restTemplate: RestTemplate) {
    companion object {
        const val KONTOREGISTER_ENDEPUNKT = "/api/system/v1/hent-aktiv-konto"
    }

    /* Denne metoden er med vilje ikke cached.
    Den brukes av bidrag-aktoerregister for å hente kontonummer på personer
    og må alltid returnere siste data. */
    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun hentKontonummer(personident: Personident): KontonummerDto? {
        val respons: ResponseEntity<KontoregisterResponse> =
            restTemplate.exchange(
                KONTOREGISTER_ENDEPUNKT,
                HttpMethod.POST,
                HttpEntity<Any>(KontoregisterRequest(personident.verdi)),
            )

        return if (respons.statusCode == HttpStatus.OK && respons.body != null) {
            konverterTilKontonummerDto(respons.body!!)
        } else {
            null
        }
    }

    private fun konverterTilKontonummerDto(response: KontoregisterResponse): KontonummerDto = if (response.utenlandskKontoInfo == null) {
        opprettNorskKontonummerDto(response)
    } else {
        opprettUtenlandskKontonummerDto(response)
    }

    private fun opprettNorskKontonummerDto(response: KontoregisterResponse): KontonummerDto = KontonummerDto(
        norskKontonr = response.kontonummer,
        metadata =
        MetadataDto(
            gyldigFom = LocalDateTime.parse(response.gyldigFom),
            opprettetAv = response.opprettetAv,
            kilde = response.kilde,
        ),
    )

    private fun opprettUtenlandskKontonummerDto(response: KontoregisterResponse): KontonummerDto = KontonummerDto(
        iban = response.kontonummer,
        swift = response.utenlandskKontoInfo?.swiftBicKode,
        banknavn = response.utenlandskKontoInfo?.banknavn,
        banklandkode = response.utenlandskKontoInfo?.bankLandkode?.let { Landkode(it) },
        bankkode = response.utenlandskKontoInfo?.bankkode,
        valutakode = response.utenlandskKontoInfo?.valutakode,
        bankadresse1 = response.utenlandskKontoInfo?.bankadresse1,
        bankadresse2 = response.utenlandskKontoInfo?.bankadresse2,
        bankadresse3 = response.utenlandskKontoInfo?.bankadresse3,
        metadata =
        MetadataDto(
            gyldigFom = LocalDateTime.parse(response.gyldigFom),
            opprettetAv = response.opprettetAv,
            kilde = response.kilde,
        ),
    )
}
