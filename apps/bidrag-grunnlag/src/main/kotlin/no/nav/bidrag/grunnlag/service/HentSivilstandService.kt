package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SivilstandGrunnlagDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto

class HentSivilstandService(private val bidragPersonConsumer: BidragPersonConsumer) {

    fun hentSivilstand(sivilstandRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<SivilstandGrunnlagDto> {
        val sivilstandListe = mutableListOf<SivilstandGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        sivilstandRequestListe.forEach {
            when (
                val restResponseSivilstand = bidragPersonConsumer.hentSivilstand(Personident(it.personId))
            ) {
                is RestResponse.Success -> {
                    leggTilSivilstand(sivilstandListe = sivilstandListe, sivilstandRespons = restResponseSivilstand.body, ident = it.personId)
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.SIVILSTAND,
                            personId = it.personId,
                            periodeFra = null,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseSivilstand.message,
                                httpStatuskode = restResponseSivilstand.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseSivilstand.message,
                                grunnlagstype = GrunnlagRequestType.SIVILSTAND,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = sivilstandListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilSivilstand(sivilstandListe: MutableList<SivilstandGrunnlagDto>, sivilstandRespons: SivilstandPdlHistorikkDto, ident: String) {
        sivilstandRespons.sivilstandPdlDto.forEach {
            sivilstandListe.add(
                SivilstandGrunnlagDto(
                    personId = ident,
                    type = it.type,
                    gyldigFom = it.gyldigFom,
                    bekreftelsesdato = it.bekreftelsesdato,
                    master = it.master,
                    registrert = it.registrert,
                    historisk = it.historisk,
                ),
            )
        }
    }
}
