package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysDto
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysResponsDto
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.KontantstøtteGrunnlagDto
import java.time.LocalDate
import java.time.YearMonth

class HentKontantstøtteService(private val familieKsSakConsumer: FamilieKsSakConsumer) {

    fun hentKontantstøtte(
        kontantstøtteRequestListe: List<PersonIdOgPeriodeRequest>,
        historiskeIdenterMap: Map<String, List<String>>,
    ): HentGrunnlagGenericDto<KontantstøtteGrunnlagDto> {
        val kontantstøtteListe = mutableListOf<KontantstøtteGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        kontantstøtteRequestListe.forEach {
            val personIdListe = historiskeIdenterMap[it.personId] ?: listOf(it.personId)

            val hentKontantstøtteRequest = BisysDto(
                fom = it.periodeFra,
                identer = personIdListe,
            )

            when (
                val restResponseKontantstøtte = familieKsSakConsumer.hentKontantstøtte(hentKontantstøtteRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilKontantstøtte(
                        kontantstøtteListe = kontantstøtteListe,
                        kontantstøtteRespons = restResponseKontantstøtte.body,
                        personIdOgPeriodeRequest = it,
                    )
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.KONTANTSTØTTE,
                            personId = it.personId,
                            periodeFra = hentKontantstøtteRequest.fom,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseKontantstøtte.message,
                                httpStatuskode = restResponseKontantstøtte.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseKontantstøtte.message,
                                grunnlagstype = GrunnlagRequestType.KONTANTSTØTTE,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = kontantstøtteListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilKontantstøtte(
        kontantstøtteListe: MutableList<KontantstøtteGrunnlagDto>,
        kontantstøtteRespons: BisysResponsDto,
        personIdOgPeriodeRequest: PersonIdOgPeriodeRequest,
    ) {
        // Kontantstøtte fra Infotrygd
        kontantstøtteRespons.infotrygdPerioder.forEach { ks ->
            val beløpPerParn = ks.beløp.div(ks.barna.size)
            ks.barna.forEach { barnPersonId ->
                // Hvis tomMåned er før fomMåned settes tomMåned lik fomMåned. Dette for å håndtere feil i gamle data i Infotrygd der tom er før fom.
                val periodeTil = if (ks.tomMåned != null && ks.tomMåned.isBefore(ks.fomMåned)) ks.fomMåned else ks.tomMåned
                kontantstøtteListe.add(
                    KontantstøtteGrunnlagDto(
                        partPersonId = personIdOgPeriodeRequest.personId,
                        barnPersonId = barnPersonId,
                        periodeFra = LocalDate.parse("${ks.fomMåned}-01"),
                        periodeTil = LocalDate.parse("$periodeTil-01").plusMonths(1) ?: null,
                        beløp = beløpPerParn,
                    ),
                )
            }
        }

        // Kontantstøtte fra ks-sak
        kontantstøtteRespons.ksSakPerioder.forEach { ks ->
            if (ks.fomMåned.isBefore(
                    YearMonth.of(personIdOgPeriodeRequest.periodeTil.year, personIdOgPeriodeRequest.periodeTil.month).plusMonths(1),
                )
            ) {
                kontantstøtteListe.add(
                    KontantstøtteGrunnlagDto(
                        partPersonId = personIdOgPeriodeRequest.personId,
                        barnPersonId = ks.barn.ident,
                        periodeFra = LocalDate.parse("${ks.fomMåned}-01"),
                        periodeTil = ks.tomMåned?.let { LocalDate.parse("$it-01").plusMonths(1) },
                        beløp = ks.barn.beløp,
                    ),
                )
            }
        }
    }
}
