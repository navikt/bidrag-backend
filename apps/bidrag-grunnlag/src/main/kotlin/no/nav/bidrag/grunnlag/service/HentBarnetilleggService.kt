package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.BarnType
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.api.BarnetilleggPensjon
import no.nav.bidrag.grunnlag.consumer.pensjon.api.HentBarnetilleggPensjonRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.BarnetilleggGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto

class HentBarnetilleggService(private val pensjonConsumer: PensjonConsumer) {

    fun hentBarnetilleggPensjon(barnetilleggPensjonRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<BarnetilleggGrunnlagDto> {
        val barnetilleggPensjonListe = mutableListOf<BarnetilleggGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        barnetilleggPensjonRequestListe.forEach {
            val hentBarnetilleggRequest = HentBarnetilleggPensjonRequest(
                mottaker = it.personId,
                fom = it.periodeFra,
                tom = it.periodeTil.minusDays(1),
            )

            when (
                val restResponseBarnetillegg = pensjonConsumer.hentBarnetilleggPensjon(hentBarnetilleggRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilBarnetillegg(
                        barnetilleggPensjonListe = barnetilleggPensjonListe,
                        barnetilleggPensjonResponsListe = restResponseBarnetillegg.body,
                        ident = it.personId,
                    )
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.BARNETILLEGG,
                            personId = hentBarnetilleggRequest.mottaker,
                            periodeFra = hentBarnetilleggRequest.fom,
                            periodeTil = hentBarnetilleggRequest.tom,
                            feiltype = evaluerFeiltype(
                                melding = restResponseBarnetillegg.message,
                                httpStatuskode = restResponseBarnetillegg.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseBarnetillegg.message,
                                grunnlagstype = GrunnlagRequestType.BARNETILLEGG,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = barnetilleggPensjonListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilBarnetillegg(
        barnetilleggPensjonListe: MutableList<BarnetilleggGrunnlagDto>,
        barnetilleggPensjonResponsListe: List<BarnetilleggPensjon>,
        ident: String,
    ) {
        barnetilleggPensjonResponsListe.forEach {
            barnetilleggPensjonListe.add(
                BarnetilleggGrunnlagDto(
                    partPersonId = ident,
                    barnPersonId = it.barn,
                    barnetilleggType = Inntektstype.BARNETILLEGG_PENSJON.toString(),
                    periodeFra = it.fom,
                    periodeTil = it.tom.plusMonths(1).withDayOfMonth(1) ?: null,
                    beløpBrutto = it.beloep,
                    barnType = if (it.erFellesbarn) BarnType.FELLES.toString() else BarnType.SÆRKULL.toString(),
                ),
            )
        }
    }
}
