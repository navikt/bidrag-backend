package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.UtvidetBarnetrygdOgSmaabarnstilleggBo
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OppdaterUtvidetBarnetrygdOgSmaabarnstillegg(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val familieBaSakConsumer: FamilieBaSakConsumer,
) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    fun oppdaterUtvidetBarnetrygdOgSmaabarnstillegg(
        utvidetBarnetrygdOgSmaabarnstilleggRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterUtvidetBarnetrygdOgSmaabarnstillegg {
        utvidetBarnetrygdOgSmaabarnstilleggRequestListe.forEach { personIdOgPeriode ->
            var antallPerioderFunnet = 0
            val familieBaSakRequest = FamilieBaSakRequest(
                personIdent = personIdOgPeriode.personId,
                fraDato = personIdOgPeriode.periodeFra,
            )

            SECURE_LOGGER.info("Kaller familie-ba-sak med request: ${tilJson(familieBaSakRequest)}")

            try {
                when (
                    val restResponseFamilieBaSak =
                        familieBaSakConsumer.hentFamilieBaSak(familieBaSakRequest)
                ) {
                    is RestResponse.Success -> {
                        val familieBaSakResponse = restResponseFamilieBaSak.body
                        SECURE_LOGGER.info("familie-ba-sak ga følgende respons: ${tilJson(familieBaSakResponse)}")
                        persistenceService.oppdaterEksisterendeUtvidetBarnetrygOgSmaabarnstilleggTilInaktiv(
                            grunnlagspakkeId = grunnlagspakkeId,
                            personIdListe = historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId),
                            timestampOppdatering = timestampOppdatering,
                        )
                        familieBaSakResponse.perioder.forEach { ubst ->
                            antallPerioderFunnet++
                            persistenceService.opprettUtvidetBarnetrygdOgSmaabarnstillegg(
                                UtvidetBarnetrygdOgSmaabarnstilleggBo(
                                    grunnlagspakkeId = grunnlagspakkeId,
                                    personId = personIdOgPeriode.personId,
                                    type = ubst.stønadstype.toString(),
                                    periodeFra = LocalDate.parse(ubst.fomMåned.toString() + "-01"),
                                    // justerer frem tildato med én dag for å ha lik logikk som resten av appen. Tildato skal angis som til, men ikke inkludert, dato.
                                    periodeTil = if (ubst.tomMåned != null) {
                                        LocalDate.parse(ubst.tomMåned.toString() + "-01")
                                            .plusMonths(1)
                                    } else {
                                        null
                                    },
                                    brukFra = timestampOppdatering,
                                    belop = BigDecimal.valueOf(ubst.beløp),
                                    manueltBeregnet = ubst.manueltBeregnet,
                                    deltBosted = ubst.deltBosted,
                                    hentetTidspunkt = timestampOppdatering,
                                ),
                            )
                        }
                        this.add(
                            OppdaterGrunnlagDto(
                                GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                                personIdOgPeriode.personId,
                                GrunnlagRequestStatus.HENTET,
                                "Antall perioder funnet: $antallPerioderFunnet",
                            ),
                        )
                    }

                    is RestResponse.Failure -> this.add(
                        OppdaterGrunnlagDto(
                            GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                            personIdOgPeriode.personId,
                            if (restResponseFamilieBaSak.statusCode == HttpStatus.NOT_FOUND) {
                                GrunnlagRequestStatus.IKKE_FUNNET
                            } else {
                                GrunnlagRequestStatus.FEILET
                            },
                            "Feil ved henting av familie-ba-sak for perioden: ${personIdOgPeriode.periodeFra} - " +
                                "${personIdOgPeriode.periodeTil}.",
                        ),
                    )
                }
            } catch (e: Exception) {
                this.add(
                    OppdaterGrunnlagDto(
                        type = GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                        personId = personIdOgPeriode.personId,
                        status = GrunnlagRequestStatus.FEILET,
                        statusMelding = "Feil ved henting av utvidet barnetrygd og småbarnstillegg for $familieBaSakRequest. Exception: ${e.message}",
                    ),
                )
            }
        }
        return this
    }
}
