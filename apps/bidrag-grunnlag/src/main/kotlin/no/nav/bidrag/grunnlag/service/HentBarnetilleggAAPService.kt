package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.BarnType
import no.nav.bidrag.grunnlag.consumer.aap.AapConsumer
import no.nav.bidrag.grunnlag.consumer.aap.api.HentBarnetilleggAAPRequest
import no.nav.bidrag.grunnlag.consumer.aap.api.HentBarnetilleggAAPResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.BarnetilleggGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto

class HentBarnetilleggAAPService(private val aapConsumer: AapConsumer) {

    fun hentBarnetillegg(request: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<BarnetilleggGrunnlagDto> {
        val barnetilleggAapListe = mutableListOf<BarnetilleggGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        request.forEach {
            val hentBarnetilleggRequest = HentBarnetilleggAAPRequest(
                personidentifikator = it.personId,
            )

            when (
                val restResponseBarnetillegg = aapConsumer.hentBarnetillegg(hentBarnetilleggRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilBarnetilleggAap(
                        response = barnetilleggAapListe,
                        barnetilleggAapResponsListe = restResponseBarnetillegg.body,
                        ident = it.personId,
                    )
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.BARNETILLEGG,
                            personId = hentBarnetilleggRequest.personidentifikator,
                            periodeFra = null,
                            periodeTil = null,
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

        return HentGrunnlagGenericDto(grunnlagListe = barnetilleggAapListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilBarnetilleggAap(
        response: MutableList<BarnetilleggGrunnlagDto>,
        barnetilleggAapResponsListe: HentBarnetilleggAAPResponse,
        ident: String,
    ) {
        barnetilleggAapResponsListe.barnMedBarnetillegg.forEach { barn ->
            response.add(
                BarnetilleggGrunnlagDto(
                    partPersonId = ident,
                    barnPersonId = barn.ident,
                    barnetilleggType = Inntektstype.BARNETILLEGG_AAP.toString(),
                    periodeFra = barn.perioderMedBarnetillegg.first().fra,
                    periodeTil = barn.perioderMedBarnetillegg.first().til,
                    beløpBrutto = barn.perioderMedBarnetillegg.first().beløp,
                    barnType = null,
                ),
            )
        }
    }
}
