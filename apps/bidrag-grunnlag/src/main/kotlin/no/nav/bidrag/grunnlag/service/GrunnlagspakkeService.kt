package no.nav.bidrag.grunnlag.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.request.OppdaterGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagspakkeDto
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagspakkeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class GrunnlagspakkeService(
    private val persistenceService: PersistenceService,
    private val oppdaterGrunnlagspakkeService: OppdaterGrunnlagspakkeService,
    private val meterRegistry: MeterRegistry,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {

    fun opprettGrunnlagspakkeCounter(formål: Formål) = Counter.builder("opprett_grunnlagspakke")
        .tag("formaal", formål.name)
        .tag("opprettetAvApp", TokenUtils.hentApplikasjonsnavn() ?: "UKJENT")
        .register(meterRegistry)

    fun opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto: OpprettGrunnlagspakkeRequestDto): Int {
        val opprettetGrunnlagspakke =
            persistenceService.opprettNyGrunnlagspakke(opprettGrunnlagspakkeRequestDto)
        opprettGrunnlagspakkeCounter(opprettGrunnlagspakkeRequestDto.formaal).increment()
        return opprettetGrunnlagspakke.grunnlagspakkeId
    }

    fun oppdaterGrunnlagspakke(
        grunnlagspakkeId: Int,
        oppdaterGrunnlagspakkeRequestDto: OppdaterGrunnlagspakkeRequestDto,
    ): OppdaterGrunnlagspakkeDto {
        val timestampOppdatering = LocalDateTime.now()

        // Validerer at grunnlagspakke eksisterer
        persistenceService.validerGrunnlagspakke(grunnlagspakkeId)

        // Henter aktiv ident for personer i grunnlagspakken
        val historiskeIdenterMap = hentHistoriskeOgAktiveIdenter(oppdaterGrunnlagspakkeRequestDto)

        // Oppdaterer aktiv ident for personer i grunnlagspakken
        val requestMedNyesteIdenter = byttUtIdentMedAktivIdent(oppdaterGrunnlagspakkeRequestDto, historiskeIdenterMap)

        val oppdaterGrunnlagspakkeDto = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeId = grunnlagspakkeId,
            oppdaterGrunnlagspakkeRequestDto = requestMedNyesteIdenter,
            timestampOppdatering = timestampOppdatering,
            historiskeIdenterMap = historiskeIdenterMap,
        )

        // Oppdaterer endret_timestamp på grunnlagspakke
        if (harOppdatertGrunnlag(oppdaterGrunnlagspakkeDto.grunnlagTypeResponsListe)) {
            persistenceService.oppdaterEndretTimestamp(grunnlagspakkeId = grunnlagspakkeId, timestampOppdatering = timestampOppdatering)
        }

        return oppdaterGrunnlagspakkeDto
    }

    private fun harOppdatertGrunnlag(grunnlagTypeResponsListe: List<OppdaterGrunnlagDto>): Boolean = grunnlagTypeResponsListe.any {
        it.status == GrunnlagRequestStatus.HENTET
    }

    // Henter historiske identer for personer i grunnlagspakke-requesten. Returnerer en map hvor key = aktiv ident og
    // value = liste med historiske identer (inklusiv den aktive identen)
    private fun hentHistoriskeOgAktiveIdenter(request: OppdaterGrunnlagspakkeRequestDto): Map<String, List<String>> {
        val historiskeIdenterMap = mutableMapOf<String, List<String>>()

        request.grunnlagRequestDtoListe.forEach { grunnlagDto ->
            // Gjør ikke oppslag for personer som allerede er lagt til i map
            if (!historiskeIdenterMap.values.any { grunnlagDto.personId in it }) {
                val historiskeIdenterListe = hentIdenterFraConsumer(grunnlagDto.personId)
                if (historiskeIdenterListe.size > 1) {
                    SECURE_LOGGER.warn(
                        "Hentet historiske identer for personId: ${grunnlagDto.personId} og fikk tilbake: ${
                            tilJson(
                                historiskeIdenterListe,
                            )
                        }",
                    )
                }

                val key = historiskeIdenterListe.find { !it.historisk }?.personId
                val values = historiskeIdenterListe.map { it.personId }.sorted()

                key?.let { historiskeIdenterMap[it] = values }
            }
        }

        return historiskeIdenterMap
    }

    // Henter historiske identer for personen. Returnerer en liste med historiske identer (inklusiv den aktive identen)
    private fun hentIdenterFraConsumer(personId: String): List<HistoriskIdent> = when (val response = bidragPersonConsumer.hentPersonidenter(personident = Personident(personId), inkludereHistoriske = true)) {
        is RestResponse.Success -> {
            val personidenterResponse = response.body
            SECURE_LOGGER.info(
                "Kall til bidrag-person for å hente historiske identer for ident $personId ga følgende respons: ${tilJson(
                    personidenterResponse,
                )}",
            )
            if (personidenterResponse.isEmpty()) {
                listOf(HistoriskIdent(personId, false))
            } else {
                personidenterResponse.map { HistoriskIdent(it.ident, it.historisk) }
            }
        }

        is RestResponse.Failure -> {
            SECURE_LOGGER.warn("Feil ved kall til bidrag-person for å hente historiske identer for ident $personId. Respons = $response")
            listOf(HistoriskIdent(personId, false))
        }
    }

    // Bytter ut identer i grunnlagspakke-requesten med aktiv ident for personen
    private fun byttUtIdentMedAktivIdent(
        request: OppdaterGrunnlagspakkeRequestDto,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterGrunnlagspakkeRequestDto {
        val dtoListe = request.grunnlagRequestDtoListe.map { grunnlagRequestDto ->
            // Søker gjennom value-listen og setter aktivIdent lik tilhørende key-verdi. Hvis personId ikke finnes i lista settes aktivIdent
            // lik personId.
            val aktivIdent = historiskeIdenterMap.entries.find { grunnlagRequestDto.personId in it.value }?.key ?: grunnlagRequestDto.personId
            if (aktivIdent != grunnlagRequestDto.personId) {
                SECURE_LOGGER.info("Hentet nyeste ident for personId: ${grunnlagRequestDto.personId} og fikk tilbake: $aktivIdent")
            }
            grunnlagRequestDto.copy(personId = aktivIdent)
        }
        return OppdaterGrunnlagspakkeRequestDto(gyldigTil = request.gyldigTil, grunnlagRequestDtoListe = dtoListe)
    }

    fun hentGrunnlagspakke(grunnlagspakkeId: Int): HentGrunnlagspakkeDto {
        // Validerer at grunnlagspakke eksisterer
        persistenceService.validerGrunnlagspakke(grunnlagspakkeId)
        return HentGrunnlagspakkeDto(
            grunnlagspakkeId = grunnlagspakkeId,
            ainntektListe = persistenceService.hentAinntekt(grunnlagspakkeId),
            skattegrunnlagListe = persistenceService.hentSkattegrunnlag(grunnlagspakkeId),
            ubstListe = persistenceService.hentUtvidetBarnetrygdOgSmaabarnstillegg(grunnlagspakkeId),
            barnetilleggListe = persistenceService.hentBarnetillegg(grunnlagspakkeId),
            kontantstotteListe = persistenceService.hentKontantstotte(grunnlagspakkeId),
            husstandmedlemmerOgEgneBarnListe = persistenceService.hentHusstandsmedlemmerOgEgneBarn(grunnlagspakkeId),

            sivilstandListe = persistenceService.hentSivilstand(grunnlagspakkeId),
            barnetilsynListe = persistenceService.hentBarnetilsyn(grunnlagspakkeId),
        )
    }

    fun lukkGrunnlagspakke(grunnlagspakkeId: Int): Int {
        // Validerer at grunnlagspakke eksisterer
        persistenceService.validerGrunnlagspakke(grunnlagspakkeId)
        return persistenceService.lukkGrunnlagspakke(grunnlagspakkeId)
    }
}

data class PersonIdOgPeriodeRequest(val personId: String, val periodeFra: LocalDate, val periodeTil: LocalDate)

data class HistoriskIdent(val personId: String, val historisk: Boolean)
