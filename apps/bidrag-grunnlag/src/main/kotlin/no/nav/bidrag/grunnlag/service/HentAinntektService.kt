package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.Aktoer
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.ArbeidsInntektMaanedIntern
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeRequest
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektRequest
import no.nav.bidrag.grunnlag.exception.custom.UgyldigInputException
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.JANUAR2015
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.erBnrEllerNpid
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeilmelding
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.evaluerFeiltype
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.finnFilter
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.finnFormaal
import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektspostDto
import no.nav.bidrag.transport.behandling.grunnlag.response.FeilrapporteringDto
import no.nav.tjenester.aordningen.inntektsinformasjon.AktoerType
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class HentAinntektService(private val inntektskomponentenService: InntektskomponentenService) {

    companion object {
        @JvmStatic
        val LOGGER: Logger = LoggerFactory.getLogger(HentAinntektService::class.java)
    }

    fun hentAinntekt(ainntektRequestListe: List<PersonIdOgPeriodeRequest>, formål: Formål): HentGrunnlagGenericDto<AinntektGrunnlagDto> {
        val ainntektListe = mutableListOf<AinntektGrunnlagDto>()
        val feilrapporteringListe = mutableListOf<FeilrapporteringDto>()

        ainntektRequestListe.forEach {
            // Hvis ident er BNR eller NPID finnes det ikke inntekter i AINNTEKT. Kaller derfor ikke Inntektskomponenten.
            if (erBnrEllerNpid(it.personId)) {
                SECURE_LOGGER.warn("Ident er BNR eller NPID, ingen inntekter funnet for ${it.personId}")
                return@forEach
            }

            val periodeFra = kalkulerPeriodeFra(it)
            val hentInntektListeRequest = lagInntektListeRequest(
                HentInntektRequest(
                    ident = it.personId,
                    maanedFom = periodeFra,
                    maanedTom = it.periodeTil.minusDays(1).toString().substring(0, 7),
                    ainntektsfilter = finnFilter(formål.name),
                    formaal = finnFormaal(formål.name),
                ),
            )

            hentInntektListeRequest.forEach { hentInntektRequest ->
                hentInntekter(hentInntektRequest = hentInntektRequest, ainntektListe = ainntektListe, feilrapporteringListe = feilrapporteringListe)
            }
        }

        return HentGrunnlagGenericDto(grunnlagListe = ainntektListe, feilrapporteringListe = feilrapporteringListe)
    }

    private fun kalkulerPeriodeFra(personIdOgPeriode: PersonIdOgPeriodeRequest): String = if (personIdOgPeriode.periodeFra.isBefore(LocalDate.parse("2015-01-01"))) {
        LOGGER.warn("Ikke tillatt med periodeFra tidligere enn 2015 i request til Ainntekt, overstyres til januar 2015")
        JANUAR2015
    } else {
        personIdOgPeriode.periodeFra.toString().substring(0, 7)
    }

    private fun hentInntekter(
        hentInntektRequest: HentInntektListeRequest,
        ainntektListe: MutableList<AinntektGrunnlagDto>,
        feilrapporteringListe: MutableList<FeilrapporteringDto>,
    ) {
        val hentInntektListeResponseIntern = inntektskomponentenService.hentInntekt(hentInntektRequest)
        if (hentInntektListeResponseIntern.httpStatus.is2xxSuccessful) {
            if (!hentInntektListeResponseIntern.arbeidsInntektMaanedIntern.isNullOrEmpty()) {
                hentInntektListeResponseIntern.arbeidsInntektMaanedIntern.forEach {
                    if (!it.arbeidsInntektInformasjonIntern.inntektIntern.isNullOrEmpty()) {
                        leggTilInntekt(inntektPeriode = it, ainntektListe = ainntektListe, ident = hentInntektRequest.ident.identifikator)
                    }
                }
            } else {
                // Hvis responsen er tom og httpStatus er 2xx, så er det ikke funnet inntekter for perioden. Ingen inntekter legges til.
                SECURE_LOGGER.warn(
                    "Ingen inntekter funnet for perioden ${hentInntektRequest.maanedFom} - ${hentInntektRequest.maanedTom} " +
                        "for ${hentInntektRequest.ident.identifikator}",
                )
            }
        } else {
            feilrapporteringListe.add(
                FeilrapporteringDto(
                    grunnlagstype = GrunnlagRequestType.AINNTEKT,
                    personId = hentInntektRequest.ident.identifikator,
                    periodeFra = hentInntektRequest.maanedFom.atDay(1),
                    periodeTil = hentInntektRequest.maanedTom.plusMonths(1).atDay(1),
                    feiltype = evaluerFeiltype(
                        melding = hentInntektListeResponseIntern.melding,
                        httpStatuskode = hentInntektListeResponseIntern.httpStatus,
                    ),
                    feilmelding = evaluerFeilmelding(melding = hentInntektListeResponseIntern.melding, grunnlagstype = GrunnlagRequestType.AINNTEKT),
                ),
            )
        }
    }

    private fun leggTilInntekt(inntektPeriode: ArbeidsInntektMaanedIntern, ainntektListe: MutableList<AinntektGrunnlagDto>, ident: String) {
        val inntektspostListe = mutableListOf<AinntektspostDto>()
        inntektPeriode.arbeidsInntektInformasjonIntern.inntektIntern?.forEach {
            inntektspostListe.add(
                AinntektspostDto(
                    utbetalingsperiode = it.utbetaltIMaaned,
                    opptjeningsperiodeFra = it.opptjeningsperiodeFom,
                    opptjeningsperiodeTil = if (it.opptjeningsperiodeTom != null) {
                        it.opptjeningsperiodeTom.plusMonths(1).withDayOfMonth(1)
                    } else {
                        null
                    },
                    opplysningspliktigId = it.opplysningspliktig?.identifikator,
                    virksomhetId = it.virksomhet?.identifikator,
                    inntektType = it.inntektType,
                    fordelType = it.fordel,
                    beskrivelse = it.beskrivelse,
                    belop = it.beloep,
                    etterbetalingsperiodeFra = it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.etterbetalingsperiodeFom,
                    etterbetalingsperiodeTil = it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.etterbetalingsperiodeTom,
                ),
            )
        }

        val inntekt = AinntektGrunnlagDto(
            personId = ident,
            periodeFra = LocalDate.parse(inntektPeriode.aarMaaned + "-01"),
            periodeTil = LocalDate.parse(inntektPeriode.aarMaaned + "-01").plusMonths(1),
            ainntektspostListe = inntektspostListe,
        )

        ainntektListe.add(inntekt)
    }

    private fun lagInntektListeRequest(hentInntektRequest: HentInntektRequest): List<HentInntektListeRequest> {
        var maanedFom = lagYearMonth(hentInntektRequest.maanedFom)
        var aarFom = maanedFom.year
        val aarTom = lagYearMonth(hentInntektRequest.maanedTom).year
        val requestListe = ArrayList<HentInntektListeRequest>()
        while (aarFom < aarTom) {
            requestListe.add(
                HentInntektListeRequest(
                    ident = Aktoer(identifikator = hentInntektRequest.ident, aktoerType = AktoerType.NATURLIG_IDENT.name),
                    maanedFom = maanedFom,
                    maanedTom = YearMonth.of(maanedFom.year, 12),
                    ainntektsfilter = hentInntektRequest.ainntektsfilter,
                    formaal = hentInntektRequest.formaal,
                ),
            )
            aarFom++
            maanedFom = YearMonth.of(aarFom, 1)
        }
        requestListe.add(
            HentInntektListeRequest(
                ident = Aktoer(identifikator = hentInntektRequest.ident, aktoerType = AktoerType.NATURLIG_IDENT.name),
                maanedFom = maanedFom,
                maanedTom = lagYearMonth(hentInntektRequest.maanedTom),
                ainntektsfilter = hentInntektRequest.ainntektsfilter,
                formaal = hentInntektRequest.formaal,
            ),
        )
        return requestListe
    }

    private fun lagYearMonth(aarMaanedString: String): YearMonth {
        if (aarMaanedString.length != 7) {
            throw UgyldigInputException("Ugyldig input i aarMaaned (må være på format ÅÅÅÅ-MM): $aarMaanedString")
        }
        return if (StringUtils.isNumeric(aarMaanedString.substring(0, 4)) && StringUtils.isNumeric(aarMaanedString.substring(5, 7))) {
            YearMonth.of(aarMaanedString.substring(0, 4).toInt(), aarMaanedString.substring(5, 7).toInt())
        } else {
            throw UgyldigInputException("Ugyldig input i aarMaaned (må være på format ÅÅÅÅ-MM): $aarMaanedString")
        }
    }
}
