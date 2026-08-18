package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.SjekkForNyIdent
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.consumer.BidragSakConsumer
import no.nav.bidrag.regnskap.dto.patch.OppdaterUtsattTilDatoRequest
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Service
class OppdragService(
    private val persistenceService: PersistenceService,
    private val oppdragsperiodeService: OppdragsperiodeService,
    private val konteringService: KonteringService,
    private val bidragSakConsumer: BidragSakConsumer,
    private val personhendelseService: PersonhendelseService,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {

    @Transactional
    fun lagreHendelse(hendelse: Hendelse, erEngangsbeløp: Boolean = false): Int? {
        val oppdrag = hentOppdrag(hendelse)

        return lagreEllerOppdaterOppdrag(oppdrag, hendelse, erEngangsbeløp)
    }

    fun lagreEllerOppdaterOppdrag(hentetOppdrag: Oppdrag?, hendelse: Hendelse, erEngangsbeløp: Boolean): Int? {
        // Dette er en edge case hvor vedtak som inneholder gebyrfritak, eller andre stønadstyper som kun blir sendt inn med avslag, kommer med beløp null og ikke eksisterer fra før av. Disse skal ikke opprettes.
        if (hentetOppdrag == null && hendelse.periodeListe.all { it.beløp == null || it.beløp.compareTo(BigDecimal.ZERO) == 0 }) { //
            LOGGER.info { "Hendelse for vedtak: ${hendelse.vedtakId} har fått fritak for ${hendelse.type}" }
            return null
        }

        val erOppdatering = hentetOppdrag != null
        val oppdrag = hentetOppdrag ?: opprettOppdrag(hendelse)
        val sisteOverførtePeriode = persistenceService.finnSisteOverførtePeriode()

        // For oppdateringer på engangsbeløp skal fra og til dato være lik det opprinnelige engangsbeløpet.
        if (erOppdatering && erEngangsbeløp) {
            settNyPeriodeFraOgTilDatoForOppdateringPåEngangsbeløp(hendelse, hentetOppdrag)
        }

        behandlePerioder(hendelse, oppdrag, erOppdatering, sisteOverførtePeriode)

        oppdatererVerdierPåOppdrag(hendelse, oppdrag)
        val oppdragId = persistenceService.lagreOppdrag(oppdrag)

        LOGGER.debug { "Oppdrag med ID: $oppdragId er ${if (erOppdatering) "oppdatert." else "opprettet."}" }

        return oppdragId
    }

    private fun behandlePerioder(
        hendelse: Hendelse,
        oppdrag: Oppdrag,
        erOppdatering: Boolean,
        sisteOverførtePeriode: YearMonth,
    ) {
        hendelse.periodeListe.forEach { periode ->
            val oppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(hendelse, periode, oppdrag)

            if (erOppdatering) {
                konteringService.opprettKorreksjonskonteringer(oppdrag, oppdragsperiode, sisteOverførtePeriode, hendelse)
                konteringService.opprettManglendeKonteringerVedOppstartAvOpphørtOppdragsperiode(
                    oppdrag,
                    oppdragsperiode,
                    sisteOverførtePeriode,
                    hendelse,
                )
            }
            oppdragsperiodeService.oppdaterAktivTilDato(oppdrag, oppdragsperiode.periodeFra)
            oppdrag.oppdragsperioder = oppdrag.oppdragsperioder.plus(oppdragsperiode)
            konteringService.opprettNyeKonteringerPåOppdragsperiode(
                oppdragsperiode,
                hendelse,
                sisteOverførtePeriode,
            )
        }
    }

    private fun settNyPeriodeFraOgTilDatoForOppdateringPåEngangsbeløp(hendelse: Hendelse, hentetOppdrag: Oppdrag?) {
        hendelse.periodeListe.forEach {
            it.periodeFomDato = hentetOppdrag!!.oppdragsperioder.first().periodeFra
            it.periodeTilDato = hentetOppdrag.oppdragsperioder.first().periodeTil
        }
    }

    private fun hentOppdrag(hendelse: Hendelse): Oppdrag? {
        if (hendelse.referanse != null && hendelse.omgjørVedtakId != null) {
            return persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId(hendelse.referanse, hendelse.omgjørVedtakId)
        } else if (hendelse.referanse == null) {
            var oppdragPåUnikeIdentifikatorer = persistenceService.hentOppdragPaUnikeIdentifikatorer(
                hendelse.type,
                hendelse.kravhaverIdent,
                hendelse.skyldnerIdent,
                hendelse.sakId,
            )

            if (oppdragPåUnikeIdentifikatorer == null) {
                val kravhaversIdenter = hendelse.kravhaverIdent?.let { bidragPersonConsumer.hentAlleIdenterForPerson(it) } ?: emptyList()
                val skyldnersIdenter = bidragPersonConsumer.hentAlleIdenterForPerson(hendelse.skyldnerIdent)

                for (skyldner in skyldnersIdenter) {
                    for (kravhaver in kravhaversIdenter) {
                        secureLogger.info { "Søker oppdrag på historiske identer. Kravhaver: ${kravhaver.ident}, Skyldner: ${skyldner.ident}" }
                        oppdragPåUnikeIdentifikatorer = persistenceService.hentOppdragPaUnikeIdentifikatorer(
                            hendelse.type,
                            kravhaver.ident,
                            skyldner.ident,
                            hendelse.sakId,
                        )
                        if (oppdragPåUnikeIdentifikatorer != null) {
                            if (kravhaver.ident != hendelse.kravhaverIdent) {
                                LOGGER.info { "Kravhaver har fått ny ident, forsøker å oppdatere eksisterende kravhavere før oppdrag hentes på nytt." }
                                personhendelseService.oppdaterKravhaver(kravhaver.ident, hendelse.kravhaverIdent!!)
                            }
                            if (skyldner.ident != hendelse.skyldnerIdent) {
                                LOGGER.info { "Skyldner har fått ny ident, forsøker å oppdatere eksisterende skyldnere før oppdrag hentes på nytt." }
                                personhendelseService.oppdaterSkyldner(skyldner.ident, hendelse.skyldnerIdent)
                            }
                            return oppdragPåUnikeIdentifikatorer
                        }
                    }
                }
            }
            return oppdragPåUnikeIdentifikatorer
        }
        return null
    }

    private fun opprettOppdrag(hendelse: Hendelse): Oppdrag {
        LOGGER.debug { "Fant ikke eksisterende oppdrag for vedtakID: ${hendelse.vedtakId}. Opprettet nytt oppdrag.." }
        return Oppdrag(
            stønadType = hendelse.type,
            sakId = hendelse.sakId,
            kravhaverIdent = hendelse.kravhaverIdent,
            skyldnerIdent = hendelse.skyldnerIdent,
            gjelderIdent = bidragSakConsumer.hentBmFraSak(hendelse.sakId),
            mottakerIdent = hendelse.mottakerIdent,
            utsattTilDato = hendelse.utsattTilDato,
        )
    }

    fun oppdatererVerdierPåOppdrag(hendelse: Hendelse, oppdrag: Oppdrag) {
        oppdrag.endretTidspunkt = LocalDateTime.now()
        // Utsatt til dato skal ikke kunne forkortes eller fjernes om først satt via vedtak
        if (oppdrag.utsattTilDato == null || hendelse.utsattTilDato?.isAfter(oppdrag.utsattTilDato) == true) {
            oppdrag.utsattTilDato = hendelse.utsattTilDato
        }
        oppdrag.mottakerIdent = hendelse.mottakerIdent
        oppdrag.gjelderIdent = bidragSakConsumer.hentBmFraSak(hendelse.sakId)
        oppdrag.kravhaverIdent = hendelse.kravhaverIdent
        oppdrag.skyldnerIdent = hendelse.skyldnerIdent
    }

    @Transactional
    fun patchMottaker(saksnummer: Saksnummer, @SjekkForNyIdent kravhaver: Personident, mottaker: Personident) {
        val oppdragPåSaksnummerOgKravhaver = persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver)

        val stønadstyperSomSkalOppdatereMedBmEllerRm = arrayOf(
            Stønadstype.FORSKUDD.name,
            Stønadstype.BIDRAG18AAR.name,
            Stønadstype.BIDRAG.name,
            Stønadstype.OPPFOSTRINGSBIDRAG.name,
            Stønadstype.MOTREGNING.name,
            Engangsbeløptype.SÆRBIDRAG.name,
            Engangsbeløptype.SÆRTILSKUDD.name,
            Engangsbeløptype.SAERTILSKUDD.name,
            Engangsbeløptype.ETTERGIVELSE.name,
            Engangsbeløptype.DIREKTE_OPPGJØR.name,
            Engangsbeløptype.DIREKTE_OPPGJOR.name,
        )

        val stønaderSomSkalOppdatereTilNavIdent = arrayOf(
            Engangsbeløptype.GEBYR_MOTTAKER.name,
            Engangsbeløptype.GEBYR_SKYLDNER.name,
            Engangsbeløptype.TILBAKEKREVING.name,
            Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING.name,
        )

        oppdragPåSaksnummerOgKravhaver.forEach { oppdrag ->
            when (oppdrag.stønadType) {
                in stønadstyperSomSkalOppdatereMedBmEllerRm -> {
                    oppdrag.mottakerIdent = mottaker.verdi
                }

                in stønaderSomSkalOppdatereTilNavIdent -> {
                    oppdrag.mottakerIdent = IdentUtils.NAV_TSS_IDENT
                }

                Stønadstype.EKTEFELLEBIDRAG.name -> {
                    oppdrag.mottakerIdent = oppdrag.kravhaverIdent ?: ""
                }
            }
        }
    }

    @Transactional
    fun oppdaterUtsattTilDato(oppdaterUtsattTilDatoRequest: OppdaterUtsattTilDatoRequest): Int? {
        val oppdrag = persistenceService.hentOppdrag(oppdaterUtsattTilDatoRequest.oppdragsid) ?: return null

        oppdrag.utsattTilDato = oppdaterUtsattTilDatoRequest.nyUtsattTilDato

        return oppdrag.oppdragId
    }
}
