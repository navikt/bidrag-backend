package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.InntektskomponentenConsumer
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.ArbeidsInntektInformasjonIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.ArbeidsInntektMaanedIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeRequest
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeResponseIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.InntektIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.OpplysningspliktigIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.TilleggsinformasjonDetaljerIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.TilleggsinformasjonIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.VirksomhetIntern
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektMaaned
import no.nav.tjenester.aordningen.inntektsinformasjon.tilleggsinformasjondetaljer.Etterbetalingsperiode
import no.nav.tjenester.aordningen.inntektsinformasjon.tilleggsinformasjondetaljer.TilleggsinformasjonDetaljerType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service

@Service
class InntektskomponentenService(private val inntektskomponentenConsumer: InntektskomponentenConsumer) {
    companion object {
        @JvmStatic
        val LOGGER: Logger = LoggerFactory.getLogger(InntektskomponentenService::class.java)
    }

    // Kaller inntektskomponenten. Prøver først å hente abonnerte inntekter. Hvis det feiler og man spør på en dato bakover i tid kastes exception.
    // Ellers gjøres det et forsøk på å kalle hentInntektListe (uten abonnement)
    fun hentInntekt(inntektListeRequest: HentInntektListeRequest): HentInntektListeResponseIntern {
        val hentInntektListeResponse = ArrayList<ArbeidsInntektMaaned>()
        var httpStatus: HttpStatusCode = HttpStatus.OK
        var melding = ""
        var exceptionKastet = false

        // Hent abonnerte inntekter
        try {
            when (val restResponseInntekt = inntektskomponentenConsumer.hentInntekter(inntektListeRequest, true)) {
                is RestResponse.Success -> {
                    // Respons OK
                    val abonnerteInntekter = restResponseInntekt.body
                    if (null != abonnerteInntekter.arbeidsInntektMaaned) {
                        hentInntektListeResponse.addAll(abonnerteInntekter.arbeidsInntektMaaned)
                    }
                }

                is RestResponse.Failure -> {
                    // Respons ikke OK. Gjør nytt forsøk, med kall mot hentInntektListe
                    try {
                        when (val restResponse2Inntekt = inntektskomponentenConsumer.hentInntekter(inntektListeRequest, false)) {
                            is RestResponse.Success -> {
                                // Respons OK
                                val inntekter = restResponse2Inntekt.body
                                if (null != inntekter.arbeidsInntektMaaned) {
                                    hentInntektListeResponse.addAll(inntekter.arbeidsInntektMaaned)
                                }
                            }

                            is RestResponse.Failure -> {
                                httpStatus = restResponse2Inntekt.statusCode
                                melding = restResponse2Inntekt.message ?: ""
                            }
                        }
                    } catch (e: Exception) {
                        exceptionKastet = true
                        melding = "Feil ved henting av inntekter uten abonnement for ${inntektListeRequest.ident.identifikator} for perioden " +
                            "${inntektListeRequest.maanedFom} - ${inntektListeRequest.maanedTom}. ${e.message} "
                    }
                }
            }
        } catch (e: Exception) {
            exceptionKastet = true
            melding = "Feil ved henting av inntekter med abonnement for ${inntektListeRequest.ident.identifikator} for perioden " +
                "${inntektListeRequest.maanedFom} - ${inntektListeRequest.maanedTom}. ${e.message} "
        }
        return mapResponsTilInternStruktur(httpStatus, melding, hentInntektListeResponse, exceptionKastet)
    }

    private fun mapResponsTilInternStruktur(
        httpStatus: HttpStatusCode,
        melding: String,
        eksternRespons: List<ArbeidsInntektMaaned>,
        exceptionKastet: Boolean,
    ): HentInntektListeResponseIntern {
        val arbeidsInntektMaanedListe = mutableListOf<ArbeidsInntektMaanedIntern>()

        eksternRespons.forEach { arbeidsInntektMaaned ->
            val inntektInternListe = mutableListOf<InntektIntern>()
            arbeidsInntektMaaned.arbeidsInntektInformasjon?.inntektListe?.forEach { inntekt ->
                val inntektIntern = InntektIntern(
                    inntektType = inntekt.inntektType.toString(),
                    beloep = inntekt.beloep,
                    fordel = inntekt.fordel,
                    inntektsperiodetype = inntekt.inntektsperiodetype,
                    opptjeningsperiodeFom = inntekt.opptjeningsperiodeFom,
                    opptjeningsperiodeTom = inntekt.opptjeningsperiodeTom,
                    utbetaltIMaaned = inntekt.utbetaltIMaaned?.toString(),
                    opplysningspliktig = OpplysningspliktigIntern(
                        inntekt.opplysningspliktig?.identifikator,
                        inntekt.opplysningspliktig?.aktoerType.toString(),
                    ),
                    virksomhet = VirksomhetIntern(
                        inntekt.virksomhet?.identifikator,
                        inntekt.virksomhet?.aktoerType.toString(),
                    ),
                    tilleggsinformasjon = if (inntekt?.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType ==
                        TilleggsinformasjonDetaljerType.ETTERBETALINGSPERIODE
                    ) {
                        TilleggsinformasjonIntern(
                            inntekt.tilleggsinformasjon.kategori,
                            TilleggsinformasjonDetaljerIntern(
                                (inntekt.tilleggsinformasjon?.tilleggsinformasjonDetaljer as Etterbetalingsperiode).etterbetalingsperiodeFom,
                                (inntekt.tilleggsinformasjon?.tilleggsinformasjonDetaljer as Etterbetalingsperiode).etterbetalingsperiodeTom.plusDays(
                                    1,
                                ),
                            ),
                        )
                    } else {
                        null
                    },
                    beskrivelse = inntekt.beskrivelse,
                )
                inntektInternListe.add(inntektIntern)
            }
            arbeidsInntektMaanedListe.add(
                ArbeidsInntektMaanedIntern(
                    arbeidsInntektMaaned.aarMaaned.toString(),
                    ArbeidsInntektInformasjonIntern(inntektInternListe),
                ),
            )
        }
        return HentInntektListeResponseIntern(httpStatus, melding, arbeidsInntektMaanedListe, exceptionKastet)
    }
}
