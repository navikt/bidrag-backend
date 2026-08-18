package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.KontantstotteBo
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysDto
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class OppdaterKontantstotte(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val familieKsSakConsumer: FamilieKsSakConsumer,
) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    fun oppdaterKontantstotte(
        kontantstotteRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterKontantstotte {
        kontantstotteRequestListe.forEach { personIdOgPeriode ->
            var antallPerioderFunnet = 0

            // Input til tjeneste er en liste over alle (historiske) identer for en person
            val personIdListe = historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId)

            val innsynRequest = BisysDto(
                fom = personIdOgPeriode.periodeFra,
                identer = personIdListe,
            )

            SECURE_LOGGER.info("Kaller kontantstøtte med request: ${tilJson(innsynRequest)}")

            try {
                when (
                    val restResponseKontantstotte =
                        familieKsSakConsumer.hentKontantstøtte(innsynRequest)
                ) {
                    is RestResponse.Success -> {
                        val kontantstotteResponse = restResponseKontantstotte.body
                        SECURE_LOGGER.info("kontantstøtte ga følgende respons: ${tilJson(kontantstotteResponse)}")

                        persistenceService.oppdaterEksisterendeKontantstotteTilInaktiv(
                            grunnlagspakkeId = grunnlagspakkeId,
                            personIdListe = personIdListe,
                            timestampOppdatering = timestampOppdatering,
                        )

                        // Kontantstøtte fra Infotrygd
                        kontantstotteResponse.infotrygdPerioder.forEach { ks ->
                            val belopPerParn = ks.beløp.div(ks.barna.size)
                            ks.barna.forEach { barnPersonId ->
                                antallPerioderFunnet++
                                val periodeTil = if (ks.tomMåned != null && ks.tomMåned.isBefore(ks.fomMåned)) ks.fomMåned else ks.tomMåned
                                persistenceService.opprettKontantstotte(
                                    KontantstotteBo(
                                        grunnlagspakkeId = grunnlagspakkeId,
                                        partPersonId = personIdOgPeriode.personId,
                                        barnPersonId = barnPersonId,
                                        periodeFra = LocalDate.parse(ks.fomMåned.toString() + "-01"),
                                        // justerer frem tildato med én dag for å ha lik logikk som resten av appen. Tildato skal angis som til, men ikke inkludert, dato.
                                        periodeTil = if (ks.tomMåned != null) LocalDate.parse(periodeTil.toString() + "-01").plusMonths(1) else null,
                                        aktiv = true,
                                        brukFra = timestampOppdatering,
                                        belop = belopPerParn,
                                        brukTil = null,
                                        hentetTidspunkt = timestampOppdatering,
                                    ),
                                )
                            }
                        }

                        // Kontantstøtte fra ks-sak
                        kontantstotteResponse.ksSakPerioder.forEach { ks ->
                            if (ks.fomMåned.isBefore(
                                    YearMonth.of(personIdOgPeriode.periodeTil.year, personIdOgPeriode.periodeTil.month).plusMonths(1),
                                )
                            ) {
                                antallPerioderFunnet++
                                persistenceService.opprettKontantstotte(
                                    KontantstotteBo(
                                        grunnlagspakkeId = grunnlagspakkeId,
                                        partPersonId = personIdOgPeriode.personId,
                                        barnPersonId = ks.barn.ident,
                                        periodeFra = LocalDate.parse(ks.fomMåned.toString() + "-01"),
                                        // justerer frem tildato med én dag for å ha lik logikk som resten av appen. Tildato skal angis som til, men ikke inkludert, dato.
                                        periodeTil = if (ks.tomMåned != null) LocalDate.parse(ks.tomMåned.toString() + "-01").plusMonths(1) else null,
                                        aktiv = true,
                                        brukFra = timestampOppdatering,
                                        belop = ks.barn.beløp,
                                        brukTil = null,
                                        hentetTidspunkt = timestampOppdatering,
                                    ),
                                )
                            }
                        }
                        this.add(
                            OppdaterGrunnlagDto(
                                type = GrunnlagRequestType.KONTANTSTØTTE,
                                personId = personIdOgPeriode.personId,
                                status = GrunnlagRequestStatus.HENTET,
                                statusMelding = "Antall perioder funnet: $antallPerioderFunnet",
                            ),
                        )
                    }

                    is RestResponse.Failure -> {
                        this.add(
                            OppdaterGrunnlagDto(
                                GrunnlagRequestType.KONTANTSTØTTE,
                                personIdOgPeriode.personId,
                                if (restResponseKontantstotte.statusCode == HttpStatus.NOT_FOUND) {
                                    GrunnlagRequestStatus.IKKE_FUNNET
                                } else {
                                    GrunnlagRequestStatus.FEILET
                                },
                                "Feil ved henting av kontantstøtte for perioden: ${personIdOgPeriode.periodeFra} - " +
                                    "${personIdOgPeriode.periodeTil}.",
                            ),
                        )
                        SECURE_LOGGER.warn("kontantstøtte familie-ks-sak svarte med feil, respons: ${tilJson(restResponseKontantstotte)}")
                    }
                }
            } catch (e: Exception) {
                this.add(
                    OppdaterGrunnlagDto(
                        type = GrunnlagRequestType.KONTANTSTØTTE,
                        personId = personIdOgPeriode.personId,
                        status = GrunnlagRequestStatus.FEILET,
                        statusMelding = "Feil ved henting av kontantstøtte. Exception: ${e.message}",
                    ),
                )
            }
        }
        return this
    }
}
