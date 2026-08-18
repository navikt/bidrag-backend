package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Skattegrunnlagstype
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagRequest
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.erBnrEllerNpid
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SkattegrunnlagGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SkattegrunnlagspostDto
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class HentSkattegrunnlagService(private val sigrunConsumer: SigrunConsumer) {
    companion object {
        const val INNTEKTSAAR_IKKE_STØTTET = "Oppgitt inntektsår er ikke støttet"
        const val FANT_IKKE_SKATTEGRUNNLAG_PROD = "Fant ikke summert skattegrunnlag"
        const val FANT_IKKE_SKATTEGRUNNLAG_TEST = "Det finnes ikke summertskattegrunnlag"
    }

    fun hentSkattegrunnlag(skattegrunnlagRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<SkattegrunnlagGrunnlagDto> {
        val skattegrunnlagListe = mutableListOf<SkattegrunnlagGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        skattegrunnlagRequestListe.forEach {
            // Hvis ident er BNR eller NPID finnes det ikke skattegrunnlag. Kaller derfor ikke Sigrun.
            if (erBnrEllerNpid(it.personId)) {
                SECURE_LOGGER.warn("Ident er BNR eller NPID, ingen skattegrunnlag funnet for ${it.personId}")
                return@forEach
            }

            var inntektÅr = it.periodeFra.year
            val sluttÅr = it.periodeTil.year

            while (inntektÅr < sluttÅr) {
                val hentSkattegrunnlagRequest = HentSummertSkattegrunnlagRequest(
                    inntektsaar = inntektÅr.toString(),
                    personident = it.personId,
                )

                when (
                    val restResponseSkattegrunnlag = sigrunConsumer.hentSummertSkattegrunnlag(hentSkattegrunnlagRequest)
                ) {
                    is RestResponse.Success -> {
                        leggTilSkattegrunnlag(
                            skattegrunnlagListe = skattegrunnlagListe,
                            skattegrunnlagRespons = restResponseSkattegrunnlag.body,
                            ident = it.personId,
                            inntektÅr = inntektÅr,
                        )
                    }

                    is RestResponse.Failure -> {
                        // Legger ikke ut noe hvis skattegrunnlag ikke er tilgjengelig enda
                        if ((restResponseSkattegrunnlag.statusCode == HttpStatus.NOT_FOUND) &&
                            (inntektsårIkkeStøttet(restResponseSkattegrunnlag.message))
                        ) {
                            SECURE_LOGGER.warn("Skattegrunnlag er ikke tilgjengelig ennå for ${it.personId} og år $inntektÅr")

                            // Legger ut tom liste hvis det ikke finnes data
                        } else if ((
                                (restResponseSkattegrunnlag.statusCode == HttpStatus.NOT_FOUND) &&
                                    (fantIkkeSkattegrunnlag(restResponseSkattegrunnlag.message))
                                ) ||
                            (
                                (restResponseSkattegrunnlag.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) &&
                                    (fantIkkeSkattegrunnlag(restResponseSkattegrunnlag.message))
                                )
                        ) {
                            skattegrunnlagListe.add(
                                SkattegrunnlagGrunnlagDto(
                                    personId = it.personId,
                                    periodeFra = LocalDate.parse("$inntektÅr-01-01"),
                                    periodeTil = LocalDate.parse("$inntektÅr-01-01").plusYears(1),
                                    skattegrunnlagspostListe = emptyList(),
                                ),
                            )
                        } else {
                            feilrapporteringListe.add(
                                FeilrapporteringDto(
                                    grunnlagstype = GrunnlagRequestType.SKATTEGRUNNLAG,
                                    personId = hentSkattegrunnlagRequest.personident,
                                    periodeFra = LocalDate.parse("${hentSkattegrunnlagRequest.inntektsaar}-01-01"),
                                    periodeTil = LocalDate.parse("${hentSkattegrunnlagRequest.inntektsaar}-01-01").plusYears(1),
                                    feiltype = evaluerFeiltype(
                                        melding = restResponseSkattegrunnlag.message,
                                        httpStatuskode = restResponseSkattegrunnlag.statusCode,
                                    ),
                                    feilmelding = evaluerFeilmelding(
                                        melding = restResponseSkattegrunnlag.message,
                                        grunnlagstype = GrunnlagRequestType.SKATTEGRUNNLAG,
                                    ),
                                ),
                            )
                        }
                    }
                }
                inntektÅr++
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = skattegrunnlagListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilSkattegrunnlag(
        skattegrunnlagListe: MutableList<SkattegrunnlagGrunnlagDto>,
        skattegrunnlagRespons: HentSummertSkattegrunnlagResponse,
        ident: String,
        inntektÅr: Int,
    ) {
        val skattegrunnlagspostListe = mutableListOf<SkattegrunnlagspostDto>()

        // Ordinære grunnlagsposter
        skattegrunnlagRespons.grunnlag?.forEach {
            skattegrunnlagspostListe.add(
                SkattegrunnlagspostDto(
                    skattegrunnlagType = Skattegrunnlagstype.ORDINÆR.toString(),
                    inntektType = it.tekniskNavn,
                    belop = BigDecimal(it.beloep),
                ),
            )
        }

        // Svalbard grunnlagsposter
        skattegrunnlagRespons.svalbardGrunnlag?.forEach {
            skattegrunnlagspostListe.add(
                SkattegrunnlagspostDto(
                    skattegrunnlagType = Skattegrunnlagstype.SVALBARD.toString(),
                    inntektType = it.tekniskNavn,
                    belop = BigDecimal(it.beloep),
                ),
            )
        }

        val skattegrunnlag = SkattegrunnlagGrunnlagDto(
            personId = ident,
            periodeFra = LocalDate.parse("$inntektÅr-01-01"),
            periodeTil = LocalDate.parse("$inntektÅr-01-01").plusYears(1),
            skattegrunnlagspostListe = skattegrunnlagspostListe,
        )

        skattegrunnlagListe.add(skattegrunnlag)
    }

    private fun inntektsårIkkeStøttet(message: String?) = message?.contains(INNTEKTSAAR_IKKE_STØTTET) ?: false

    private fun fantIkkeSkattegrunnlag(message: String?): Boolean = message?.contains(FANT_IKKE_SKATTEGRUNNLAG_PROD) == true || message?.contains(FANT_IKKE_SKATTEGRUNNLAG_TEST) == true
}
