package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.BarnType
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.BarnetilleggBo
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.api.HentBarnetilleggPensjonRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class OppdaterBarnetillegg(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val pensjonConsumer: PensjonConsumer,
) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    fun oppdaterBarnetillegg(
        barnetilleggRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterBarnetillegg {
        barnetilleggRequestListe.forEach { personIdOgPeriode ->
            var antallPerioderFunnet = 0
            val hentBarnetilleggPensjonRequest = HentBarnetilleggPensjonRequest(
                mottaker = personIdOgPeriode.personId,
                fom = personIdOgPeriode.periodeFra,
                tom = personIdOgPeriode.periodeTil.minusDays(1),
            )

            SECURE_LOGGER.info("Kaller barnetillegg pensjon med request: ${tilJson(hentBarnetilleggPensjonRequest)}")

            try {
                when (
                    val restResponseBarnetilleggPensjon =
                        pensjonConsumer.hentBarnetilleggPensjon(hentBarnetilleggPensjonRequest)
                ) {
                    is RestResponse.Success -> {
                        val barnetilleggPensjonResponse = restResponseBarnetilleggPensjon.body

                        SECURE_LOGGER.info("Barnetillegg pensjon ga følgende respons: ${tilJson(barnetilleggPensjonResponse)}")

                        persistenceService.oppdaterEksisterendeBarnetilleggPensjonTilInaktiv(
                            grunnlagspakkeId = grunnlagspakkeId,
                            personIdListe = historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId),
                            timestampOppdatering = timestampOppdatering,
                        )
                        barnetilleggPensjonResponse.forEach { bt ->
                            antallPerioderFunnet++
                            persistenceService.opprettBarnetillegg(
                                BarnetilleggBo(
                                    grunnlagspakkeId = grunnlagspakkeId,
                                    partPersonId = personIdOgPeriode.personId,
                                    barnPersonId = bt.barn,
                                    barnetilleggType = Inntektstype.BARNETILLEGG_PENSJON.toString(),
                                    periodeFra = bt.fom,
                                    // justerer frem tildato med én dag for å ha lik logikk som resten av appen. Tildato skal angis som til, men ikke inkludert, dato.
                                    periodeTil = bt.tom.plusMonths(1)?.withDayOfMonth(1),
                                    aktiv = true,
                                    brukFra = timestampOppdatering,
                                    brukTil = null,
                                    belopBrutto = bt.beloep,
                                    barnType = if (bt.erFellesbarn) BarnType.FELLES.toString() else BarnType.SÆRKULL.toString(),
                                    hentetTidspunkt = timestampOppdatering,
                                ),
                            )
                        }
                        this.add(
                            OppdaterGrunnlagDto(
                                GrunnlagRequestType.BARNETILLEGG,
                                personIdOgPeriode.personId,
                                GrunnlagRequestStatus.HENTET,
                                "Antall perioder funnet: $antallPerioderFunnet",
                            ),
                        )
                    }

                    is RestResponse.Failure -> this.add(
                        OppdaterGrunnlagDto(
                            GrunnlagRequestType.BARNETILLEGG,
                            personIdOgPeriode.personId,
                            if (restResponseBarnetilleggPensjon.statusCode == HttpStatus.NOT_FOUND) {
                                GrunnlagRequestStatus.IKKE_FUNNET
                            } else {
                                GrunnlagRequestStatus.FEILET
                            },
                            "Feil ved henting av barnetillegg pensjon for perioden: ${personIdOgPeriode.periodeFra} - " +
                                "${personIdOgPeriode.periodeTil}.",
                        ),
                    )
                }
            } catch (e: Exception) {
                this.add(
                    OppdaterGrunnlagDto(
                        type = GrunnlagRequestType.BARNETILLEGG,
                        personId = personIdOgPeriode.personId,
                        status = GrunnlagRequestStatus.FEILET,
                        statusMelding = "Feil ved henting av barnetillegg fra pensjon. Exception: ${e.message}",
                    ),
                )
            }
        }
        return this
    }
}
