package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.EnhetsregisterConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.Arbeidsforhold
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.Arbeidssted
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentArbeidsforholdRequest
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.transport.behandling.grunnlag.response.Ansettelsesdetaljer
import no.nav.bidrag.transport.behandling.grunnlag.response.ArbeidsforholdGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.bidrag.transport.behandling.grunnlag.response.Permisjon
import no.nav.bidrag.transport.behandling.grunnlag.response.Permittering

class HentArbeidsforholdService(
    private val arbeidsforholdConsumer: ArbeidsforholdConsumer,
    private val enhetsregisterConsumer: EnhetsregisterConsumer,
) {

    fun hentArbeidsforhold(arbeidsforholdRequestListe: List<PersonIdOgPeriodeRequest>): HentGrunnlagGenericDto<ArbeidsforholdGrunnlagDto> {
        val arbeidsforholdListe = mutableListOf<ArbeidsforholdGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        arbeidsforholdRequestListe.forEach {
            val hentArbeidsforholdRequest = HentArbeidsforholdRequest(arbeidstakerId = it.personId)

            when (
                val restResponseArbeidsforhold = arbeidsforholdConsumer.hentArbeidsforhold(hentArbeidsforholdRequest)
            ) {
                is RestResponse.Success -> {
                    leggTilArbeidsforhold(
                        arbeidsforholdListe = arbeidsforholdListe,
                        feilrapporteringListe = feilrapporteringListe,
                        arbeidsforholdResponsListe = restResponseArbeidsforhold.body,
                        ident = it.personId,
                    )
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.ARBEIDSFORHOLD,
                            personId = hentArbeidsforholdRequest.arbeidstakerId,
                            periodeFra = null,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseArbeidsforhold.message,
                                httpStatuskode = restResponseArbeidsforhold.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseArbeidsforhold.message,
                                grunnlagstype = GrunnlagRequestType.ARBEIDSFORHOLD,
                            ),
                        ),
                    )
                }
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = arbeidsforholdListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun leggTilArbeidsforhold(
        arbeidsforholdListe: MutableList<ArbeidsforholdGrunnlagDto>,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
        arbeidsforholdResponsListe: List<Arbeidsforhold>,
        ident: String,
    ) {
        arbeidsforholdResponsListe.forEach { arbeidsforhold ->

            val ansettelsesdetaljerListe = mutableListOf<Ansettelsesdetaljer>()
            val permisjonListe = mutableListOf<Permisjon>()
            val permitteringListe = mutableListOf<Permittering>()
            val arbeidsgiverinfo = finnArbeidsgiverinfo(
                arbeidssted = arbeidsforhold.arbeidssted,
                feilrapporteringListe = feilrapporteringListe,
                ident = ident,
            )

            arbeidsforhold.ansettelsesdetaljer?.forEach {
                ansettelsesdetaljerListe.add(
                    Ansettelsesdetaljer(
                        periodeFra = it.rapporteringsmaaneder?.fra,
                        periodeTil = it.rapporteringsmaaneder?.til,
                        arbeidsforholdType = it.type,
                        arbeidstidsordningBeskrivelse = it.arbeidstidsordning?.beskrivelse,
                        ansettelsesformBeskrivelse = it.ansettelsesform?.beskrivelse,
                        yrkeBeskrivelse = it.yrke?.beskrivelse,
                        antallTimerPrUke = it.antallTimerPrUke,
                        avtaltStillingsprosent = it.avtaltStillingsprosent,
                        sisteStillingsprosentendringDato = it.sisteStillingsprosentendring,
                        sisteLønnsendringDato = it.sisteLoennsendring,
                    ),
                )
            }

            arbeidsforhold.permisjoner?.forEach {
                permisjonListe.add(
                    Permisjon(
                        startdato = it.startdato,
                        sluttdato = it.sluttdato,
                        beskrivelse = it.type?.beskrivelse,
                        prosent = it.prosent,
                    ),
                )
            }

            arbeidsforhold.permitteringer?.forEach {
                permitteringListe.add(
                    Permittering(
                        startdato = it.startdato,
                        sluttdato = it.sluttdato,
                        beskrivelse = it.type?.beskrivelse,
                        prosent = it.prosent,
                    ),
                )
            }

            arbeidsforholdListe.add(
                ArbeidsforholdGrunnlagDto(
                    partPersonId = ident,
                    startdato = arbeidsforhold.ansettelsesperiode?.startdato,
                    sluttdato = arbeidsforhold.ansettelsesperiode?.sluttdato,
                    arbeidsgiverNavn = arbeidsgiverinfo?.navn,
                    arbeidsgiverOrgnummer = arbeidsgiverinfo?.orgnr,
                    ansettelsesdetaljerListe = ansettelsesdetaljerListe,
                    permisjonListe = permisjonListe,
                    permitteringListe = permitteringListe,
                ),
            )
        }
    }

    private fun finnArbeidsgiverinfo(
        arbeidssted: Arbeidssted?,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
        ident: String,
    ): Arbeidsgiverinfo? = if (arbeidssted?.type == "Underenhet") {
        val orgnr = arbeidssted.identer?.filter { it.type == "ORGANISASJONSNUMMER" }?.get(0)?.ident
        var navn: String? = null

        if (orgnr != null) {
            when (val restResponseEnhetsregister = enhetsregisterConsumer.hentEnhetsinfo(HentEnhetsregisterRequest(orgnr))) {
                is RestResponse.Success -> {
                    navn = restResponseEnhetsregister.body.navn?.navnelinje1
                }

                is RestResponse.Failure -> {
                    feilrapporteringListe.add(
                        FeilrapporteringDto(
                            grunnlagstype = GrunnlagRequestType.ARBEIDSFORHOLD,
                            personId = ident,
                            periodeFra = null,
                            periodeTil = null,
                            feiltype = evaluerFeiltype(
                                melding = restResponseEnhetsregister.message,
                                httpStatuskode = restResponseEnhetsregister.statusCode,
                            ),
                            feilmelding = evaluerFeilmelding(
                                melding = restResponseEnhetsregister.message,
                                grunnlagstype = GrunnlagRequestType.ARBEIDSFORHOLD,
                            ),
                        ),
                    )
                }
            }
        }

        Arbeidsgiverinfo(
            orgnr = orgnr,
            navn = navn,
        )
    } else {
        null
    }
}

data class Arbeidsgiverinfo(val orgnr: String?, val navn: String?)
