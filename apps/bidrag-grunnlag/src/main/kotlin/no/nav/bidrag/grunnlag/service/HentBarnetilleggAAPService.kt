package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
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
import java.math.BigDecimal
import java.math.RoundingMode

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
                            grunnlagstype = GrunnlagRequestType.BARNETILLEGG_AAP,
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
                    beløpBrutto = beregnMånedsbeløpTilleggsstønad(barn.perioderMedBarnetillegg.first().beløp),
                    // TODO feltet barntype har ingen verdi fra AAP og må gjøres nullable
                    barnType = BarnType.UKJENT.toString(),
                ),
            )
        }
    }

    // TODO denne må gjennomgåes. Det kan finnes flere perioder innenfor en måned med ulike beløp. Da må vi summere beløpene for perioden og deretter beregne månedsbeløpet.
    fun beregnMånedsbeløpTilleggsstønad(beløp: BigDecimal): BigDecimal {
        val resultat =
            beløp.multiply(BigDecimal.valueOf(260)).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(11))
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP).coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO

        return resultat
    }
}
