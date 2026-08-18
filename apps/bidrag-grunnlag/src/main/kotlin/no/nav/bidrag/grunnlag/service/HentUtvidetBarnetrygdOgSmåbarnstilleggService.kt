package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import java.math.BigDecimal
import java.time.LocalDate

class HentUtvidetBarnetrygdOgSmåbarnstilleggService(private val familieBaSakConsumer: FamilieBaSakConsumer) {

    fun hentUbst(ubstRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<UtvidetBarnetrygdOgSmåbarnstilleggGrunnlagDto> {
        val ubstListe = mutableListOf<UtvidetBarnetrygdOgSmåbarnstilleggGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        ubstRequestListe.forEach {
            val hentUbstRequest = FamilieBaSakRequest(
                personIdent = it.personId,
                fraDato = it.periodeFra,
            )

            when (
                val restResponseUbst = familieBaSakConsumer.hentFamilieBaSak(hentUbstRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilUbst(ubstListe = ubstListe, familieBaSakRespons = restResponseUbst.body, ident = it.personId)
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                            personId = hentUbstRequest.personIdent,
                            periodeFra = it.periodeFra,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseUbst.message,
                                httpStatuskode = restResponseUbst.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseUbst.message,
                                grunnlagstype = GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = ubstListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilUbst(
        ubstListe: MutableList<UtvidetBarnetrygdOgSmåbarnstilleggGrunnlagDto>,
        familieBaSakRespons: FamilieBaSakResponse,
        ident: String,
    ) {
        familieBaSakRespons.perioder.forEach { periode ->
            ubstListe.add(
                UtvidetBarnetrygdOgSmåbarnstilleggGrunnlagDto(
                    personId = ident,
                    type = periode.stønadstype.toString(),
                    periodeFra = LocalDate.parse("${periode.fomMåned}-01"),
                    periodeTil = periode.tomMåned?.let { LocalDate.parse("$it-01").plusMonths(1) },
                    beløp = BigDecimal.valueOf(periode.beløp),
                    manueltBeregnet = periode.manueltBeregnet,
                ),
            )
        }
    }
}
