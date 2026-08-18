package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.TilleggsstønadRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.TilleggsstønadResponse
import no.nav.bidrag.grunnlag.consumer.tilleggsstønad.TilleggsstønadConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.TilleggsstønadGrunnlagDto

class HentTilleggsstønadService(private val tilleggsstønadConsumer: TilleggsstønadConsumer) {

    fun hentTilleggsstønad(tilleggsstønadRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<TilleggsstønadGrunnlagDto> {
        val tilleggsstønadListe = mutableListOf<TilleggsstønadGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        tilleggsstønadRequestListe.forEach {
            val hentTilleggsstønadRequest = TilleggsstønadRequest(
                ident = it.personId,
            )

            when (
                val restResponseTilleggsstønad = tilleggsstønadConsumer.hentTilleggsstønad(hentTilleggsstønadRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilTilleggsstønad(
                        tilleggsstønadListe = tilleggsstønadListe,
                        tilleggsstønadRespons = restResponseTilleggsstønad.body,
                        personIdOgPeriodeRequest = it,
                    )
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.TILLEGGSSTØNAD,
                            personId = it.personId,
                            periodeFra = it.periodeFra,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseTilleggsstønad.message,
                                httpStatuskode = restResponseTilleggsstønad.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseTilleggsstønad.message,
                                grunnlagstype = GrunnlagRequestType.TILLEGGSSTØNAD,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = tilleggsstønadListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilTilleggsstønad(
        tilleggsstønadListe: MutableList<TilleggsstønadGrunnlagDto>,
        tilleggsstønadRespons: TilleggsstønadResponse,
        personIdOgPeriodeRequest: PersonIdOgPeriodeRequest,
    ) {
        tilleggsstønadListe.add(
            TilleggsstønadGrunnlagDto(
                partPersonId = personIdOgPeriodeRequest.personId,
                harInnvilgetVedtak = tilleggsstønadRespons.harInnvilgetVedtak,
            ),
        )
    }
}
