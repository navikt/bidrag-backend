package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Skattegrunnlagstype
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import no.nav.bidrag.grunnlag.comparator.PeriodComparable
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagRequest
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.Skattegrunnlag
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.PersonIdOgPeriodeRequest
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagDto
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OppdaterSkattegrunnlag(
    private val grunnlagspakkeId: Int,
    private val timestampOppdatering: LocalDateTime,
    private val persistenceService: PersistenceService,
    private val sigrunConsumer: SigrunConsumer,
) : MutableList<OppdaterGrunnlagDto> by mutableListOf() {

    fun oppdaterSkattegrunnlag(
        skattegrunnlagRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): OppdaterSkattegrunnlag {
        skattegrunnlagRequestListe.forEach { personIdOgPeriode ->

            var inntektAar = personIdOgPeriode.periodeFra.year
            val sluttAar = personIdOgPeriode.periodeTil.year

            val periodeFra = LocalDate.of(inntektAar, 1, 1)
            val periodeTil = LocalDate.of(sluttAar, 1, 1)

            val nyeSkattegrunnlag =
                mutableListOf<PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>>()

            while (inntektAar < sluttAar) {
                val skattegrunnlagRequest = HentSummertSkattegrunnlagRequest(
                    inntektsaar = inntektAar.toString(),
                    personident = personIdOgPeriode.personId,
                )

                SECURE_LOGGER.info("Kaller Sigrun (skattegrunnlag) med request: ${tilJson(skattegrunnlagRequest)}")

                try {
                    when (val restResponseSkattegrunnlag = sigrunConsumer.hentSummertSkattegrunnlag(skattegrunnlagRequest)) {
                        is RestResponse.Success -> {
                            var antallSkattegrunnlagsposter = 0
                            val skattegrunnlagResponse = restResponseSkattegrunnlag.body
                            SECURE_LOGGER.info("Sigrun (skattegrunnlag) ga følgende respons: ${tilJson(skattegrunnlagResponse)}")

                            val skattegrunnlagsPosterOrdinaer = mutableListOf<Skattegrunnlag>()
                            val skattegrunnlagsPosterSvalbard = mutableListOf<Skattegrunnlag>()
                            skattegrunnlagsPosterOrdinaer.addAll(skattegrunnlagResponse.grunnlag!!.toMutableList())
                            skattegrunnlagsPosterSvalbard.addAll(skattegrunnlagResponse.svalbardGrunnlag!!.toMutableList())

                            if (skattegrunnlagsPosterOrdinaer.isNotEmpty() || skattegrunnlagsPosterSvalbard.isNotEmpty()) {
                                val skattegrunnlag = SkattegrunnlagBo(
                                    grunnlagspakkeId = grunnlagspakkeId,
                                    personId = personIdOgPeriode.personId,
                                    periodeFra = LocalDate.parse("$inntektAar-01-01"),
                                    // justerer frem tildato med én dag for å ha lik logikk som resten av appen. Tildato skal angis som til, men ikke inkludert, dato.
                                    periodeTil = LocalDate.parse("$inntektAar-01-01").plusYears(1),
                                    brukFra = timestampOppdatering,
                                    hentetTidspunkt = timestampOppdatering,
                                )
                                val skattegrunnlagsposter = mutableListOf<SkattegrunnlagspostBo>()
                                skattegrunnlagsPosterOrdinaer.forEach { skattegrunnlagsPost ->
                                    antallSkattegrunnlagsposter++
                                    skattegrunnlagsposter.add(
                                        SkattegrunnlagspostBo(
                                            skattegrunnlagType = Skattegrunnlagstype.ORDINÆR.toString(),
                                            inntektType = skattegrunnlagsPost.tekniskNavn,
                                            belop = BigDecimal(skattegrunnlagsPost.beloep),
                                        ),
                                    )
                                }
                                skattegrunnlagsPosterSvalbard.forEach { skattegrunnlagsPost ->
                                    antallSkattegrunnlagsposter++
                                    skattegrunnlagsposter.add(
                                        SkattegrunnlagspostBo(
                                            skattegrunnlagType = Skattegrunnlagstype.SVALBARD.toString(),
                                            inntektType = skattegrunnlagsPost.tekniskNavn,
                                            belop = BigDecimal(skattegrunnlagsPost.beloep),
                                        ),
                                    )
                                }
                                nyeSkattegrunnlag.add(PeriodComparable(skattegrunnlag, skattegrunnlagsposter))
                            }
                            persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
                                grunnlagspakkeId = grunnlagspakkeId,
                                newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
                                periodeFra = periodeFra,
                                periodeTil = periodeTil,
                                personIdListe = historiskeIdenterMap[personIdOgPeriode.personId] ?: listOf(personIdOgPeriode.personId),
                                timestampOppdatering = timestampOppdatering,
                            )
                            this.add(
                                OppdaterGrunnlagDto(
                                    type = GrunnlagRequestType.SKATTEGRUNNLAG,
                                    personId = personIdOgPeriode.personId,
                                    status = GrunnlagRequestStatus.HENTET,
                                    statusMelding = "Antall skattegrunnlagsposter funnet for inntektsåret $inntektAar: $antallSkattegrunnlagsposter",
                                ),
                            )
                        }

                        is RestResponse.Failure -> this.add(
                            OppdaterGrunnlagDto(
                                type = GrunnlagRequestType.SKATTEGRUNNLAG,
                                personId = personIdOgPeriode.personId,
                                status = if (restResponseSkattegrunnlag.statusCode == HttpStatus.NOT_FOUND) {
                                    GrunnlagRequestStatus.IKKE_FUNNET
                                } else {
                                    GrunnlagRequestStatus.FEILET
                                },
                                statusMelding = "Feil ved henting av skattegrunnlag for inntektsåret $inntektAar.",
                            ),
                        )
                    }
                    inntektAar++
                } catch (e: Exception) {
                    this.add(
                        OppdaterGrunnlagDto(
                            type = GrunnlagRequestType.SKATTEGRUNNLAG,
                            personId = personIdOgPeriode.personId,
                            status = GrunnlagRequestStatus.FEILET,
                            statusMelding = "Feil ved henting av skattegrunnlag for inntektsåret $inntektAar. Exception: ${e.message}",
                        ),
                    )
                }
            }
        }
        return this
    }
}
