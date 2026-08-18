package no.nav.bidrag.regnskap.service

import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderSøknadType
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderType
import no.nav.bidrag.regnskap.util.PeriodeUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class KonteringService(
    private val persistenceService: PersistenceService,
) {

    fun opprettNyeKonteringerPåOppdragsperiode(oppdragsperiode: Oppdragsperiode, hendelse: Hendelse, sisteOverførtePeriode: YearMonth) {
        val perioderForOppdragsperiode =
            hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(oppdragsperiode, sisteOverførtePeriode)
        val alleOppdragsperioderForOppdrag = oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList()
        val transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(hendelse.type).name
        val vedtakId = hendelse.vedtakId

        perioderForOppdragsperiode.forEachIndexed { indexPeriode, periode ->

            if (skalIkkeOppretteKonteringerForOpphør(oppdragsperiode, periode)) {
                return@forEachIndexed
            }

            val nyKontering = Kontering(
                transaksjonskode = transaksjonskode,
                overføringsperiode = periode.toString(),
                type = vurderType(alleOppdragsperioderForOppdrag, periode),
                søknadType = vurderSøknadType(hendelse, indexPeriode),
                oppdragsperiode = oppdragsperiode,
                vedtakId = vedtakId,
            )
            oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(nyKontering)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun settAlleKonteringerSomOverførtForPåløp(påløp: Påløp) {
        val tidspunkt = LocalDateTime.now()
        val påløpKjøredato = påløp.kjøredato.toLocalDate()
        val konteringer = persistenceService.hentAlleIkkeOverførteKonteringer()

        konteringer.forEach { kontering ->
            // Om konteringen er knyttet til et oppdrag som har utsattTilDato senere enn nåværende tidspunkt så er ikke konteringen med i påløpsfila.
            if (kontering.oppdragsperiode?.oppdrag?.utsattTilDato?.isAfter(påløpKjøredato) == true) {
                return@forEach
            }
            if (kontering.oppdragsperiode?.oppdrag?.harFeiledeKonteringer == true) {
                return@forEach
            }

            kontering.overføringstidspunkt = tidspunkt
            kontering.behandlingsstatusOkTidspunkt = tidspunkt
            kontering.sisteReferansekode = null
            kontering.sendtIPåløpsperiode = påløp.forPeriode
        }
    }

    // Om det opprettes et opphør og det ikke finnes eksisterende korrigerte konteringer for perioden skal det ikke opprettes ny kontering.
    private fun skalIkkeOppretteKonteringerForOpphør(
        oppdragsperiode: Oppdragsperiode,
        periode: YearMonth,
    ): Boolean {
        val opphørendeOppdragsperiode = oppdragsperiode.opphørendeOppdragsperiode
        val beløpErZero = oppdragsperiode.beløp == BigDecimal.ZERO
        return opphørendeOppdragsperiode &&
            beløpErZero &&
            !finnesKorrigerendeKonteringerForMåned(periode, oppdragsperiode)
    }

    @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
    private fun finnesKorrigerendeKonteringerForMåned(periode: YearMonth, oppdragsperiode: Oppdragsperiode): Boolean {
        val oppdragsperioder = oppdragsperiode.oppdrag!!.oppdragsperioder
        return oppdragsperioder
            .flatMap { it.konteringer }
            .any {
                YearMonth.parse(it.overføringsperiode) == periode &&
                    Transaksjonskode.valueOf(it.transaksjonskode).korreksjonskode == null
            }
    }

    fun opprettKorreksjonskonteringer(oppdrag: Oppdrag, oppdragsperiode: Oppdragsperiode, sisteOverførtePeriode: YearMonth, hendelse: Hendelse) {
        val overførteKonteringerListe = hentAlleKonteringerForOppdrag(oppdrag)
        val perioderForOppdragsperiode = PeriodeUtils.hentAllePerioderMellomDato(
            oppdragsperiode.periodeFra,
            oppdragsperiode.periodeTil,
            sisteOverførtePeriode,
        )
        opprettKorreksjonskonteringForOverførteKonteringer(
            perioderForOppdragsperiode,
            overførteKonteringerListe,
            hendelse,
        )
    }

    private fun opprettKorreksjonskonteringForOverførteKonteringer(
        perioderForOppdragsperiode: List<YearMonth>,
        overførteKonteringerListe: List<Kontering>,
        hendelse: Hendelse,
    ) {
        overførteKonteringerListe.forEach { kontering ->
            val korreksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode

            if (skalOppretteKorreksjonskontering(korreksjonskode, kontering, overførteKonteringerListe, perioderForOppdragsperiode)) {
                val nyKorreksjonskontering = Kontering(
                    oppdragsperiode = kontering.oppdragsperiode,
                    overføringsperiode = kontering.overføringsperiode,
                    transaksjonskode = korreksjonskode!!,
                    type = Type.ENDRING.name,
                    søknadType = Søknadstype.EN.name,
                    vedtakId = hendelse.vedtakId,
                )

                kontering.oppdragsperiode?.konteringer = kontering.oppdragsperiode!!.konteringer.plus(nyKorreksjonskontering)
            }
        }
    }

    private fun skalOppretteKorreksjonskontering(
        korreksjonskode: String?,
        kontering: Kontering,
        overførteKonteringerListe: List<Kontering>,
        perioderForOppdragsperiode: List<YearMonth>,
    ): Boolean = korreksjonskode != null &&
        !erOverførtKonteringAlleredeKorrigert(kontering, overførteKonteringerListe) &&
        erPeriodeOverlappendeSlutterFørOverførteKonteringsperiodeEllerGebyr(perioderForOppdragsperiode, kontering)

    private fun erPeriodeOverlappendeSlutterFørOverførteKonteringsperiodeEllerGebyr(
        perioderForOppdragsperiode: List<YearMonth>,
        kontering: Kontering,
    ): Boolean = erPeriodeOverlappende(perioderForOppdragsperiode, kontering) ||
        slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering, perioderForOppdragsperiode) ||
        erKonteringGebyr(kontering)

    private fun erKonteringGebyr(kontering: Kontering): Boolean = (kontering.søknadType == Søknadstype.FABM.name || kontering.søknadType == Søknadstype.FABP.name)

    private fun slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering: Kontering, perioderForNyOppdrasperiode: List<YearMonth>): Boolean {
        val maxOppdragsperiode = perioderForNyOppdrasperiode.maxOrNull() ?: return false
        return YearMonth.parse(kontering.overføringsperiode).isAfter(maxOppdragsperiode)
    }

    private fun erPeriodeOverlappende(perioderForNyOppdrasperiode: List<YearMonth>, kontering: Kontering): Boolean = perioderForNyOppdrasperiode.contains(YearMonth.parse(kontering.overføringsperiode))

    private fun hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth,
    ): List<YearMonth> = PeriodeUtils.hentAllePerioderMellomDato(
        oppdragsperiode.periodeFra,
        oppdragsperiode.periodeTil,
        sisteOverførtePeriode,
    ).filter { it.isBefore(sisteOverførtePeriode.plusMonths(1)) }

    private fun erOverførtKonteringAlleredeKorrigert(kontering: Kontering, overførteKonteringerListe: List<Kontering>): Boolean = overførteKonteringerListe.any {
        it.transaksjonskode == Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode &&
            it.oppdragsperiode == kontering.oppdragsperiode &&
            it.overføringsperiode == kontering.overføringsperiode
    }

    fun hentAlleKonteringerForOppdrag(oppdrag: Oppdrag): List<Kontering> = oppdrag.oppdragsperioder.flatMap { it.konteringer }

    fun opprettManglendeKonteringerVedOppstartAvOpphørtOppdragsperiode(
        oppdrag: Oppdrag,
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth,
        hendelse: Hendelse,
    ) {
        oppdrag.oppdragsperioder
            .filter { it.aktivTil == null && it.opphørendeOppdragsperiode }
            .forEach {
                val kontering = it.konteringer.maxByOrNull { kontering -> kontering.overføringsperiode } ?: return
                val nestePeriode = YearMonth.parse(kontering.overføringsperiode).plusMonths(1)
                val perioder = PeriodeUtils.hentAllePerioderMellomDato(
                    LocalDate.of(nestePeriode.year, nestePeriode.month, 1),
                    oppdragsperiode.periodeFra,
                    sisteOverførtePeriode,
                )
                perioder.forEach { periode ->
                    val nyKontering = Kontering(
                        oppdragsperiode = kontering.oppdragsperiode,
                        overføringsperiode = periode.toString(),
                        transaksjonskode = kontering.transaksjonskode,
                        type = Type.NY.name,
                        søknadType = kontering.søknadType,
                        vedtakId = hendelse.vedtakId,
                    )
                    it.konteringer = it.konteringer.plus(nyKontering)
                }
            }
    }
}
