package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.util.PåløpException
import no.nav.bidrag.transport.behandling.vedtak.Engangsbeløp
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val LOGGER = KotlinLogging.logger {}
private val objectMapper =
    ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Service
class VedtakshendelseService(
    private val oppdragService: OppdragService,
    private val oppdragsperiodeService: OppdragsperiodeService,
    private val kravService: KravService,
    private val persistenceService: PersistenceService,
    private val identUtils: IdentUtils,
    private val driftsavvikService: DriftsavvikService,
) {

    @Transactional
    fun behandleHendelse(hendelse: String): List<Int> {
        if (driftsavvikService.harAktivtDriftsavvik(erInnlesing = true)) {
            throw PåløpException("Det finnes aktive driftsavvik som ikke tillater innlesing. Kan derfor ikke opprette oppdrag for vedtakshendelse.")
        }

        val vedtakHendelse = mapVedtakHendelse(hendelse)

        if (oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(vedtakHendelse.id).isNotEmpty()) {
            LOGGER.warn { "VedtakHendelse med vedtakid: ${vedtakHendelse.id} er allerede behandlet. Ignorerer hendelse." }
            return emptyList()
        }

        LOGGER.info { "Behandler vedakHendelse for vedtakid: ${vedtakHendelse.id}" }

        val opprettedeOppdrag = mutableListOf<Int>()

        vedtakHendelse.stønadsendringListe?.forEach { stønadsendring ->
            opprettOppdragForStønadsending(vedtakHendelse, stønadsendring)?.let {
                opprettedeOppdrag.add(it)
            }
        }

        vedtakHendelse.engangsbeløpListe?.forEach { engangsbelop ->
            opprettOppdragForEngangsbeløp(vedtakHendelse, engangsbelop)?.let {
                opprettedeOppdrag.add(it)
            }
        }

        return opprettedeOppdrag
    }

    fun mapVedtakHendelse(hendelse: String): VedtakHendelse = try {
        objectMapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
        secureLogger.debug { "${"Leser hendelse: {}"} $hendelse" }
    }

    private fun opprettOppdragForStønadsending(vedtakHendelse: VedtakHendelse, stønadsendring: Stønadsendring): Int? {
        LOGGER.debug { "Lagrer hendelse for stønadendring." }

        if (erInnkrevingOgEndring("Stønadsendring (vedtakId: ${vedtakHendelse.id}, sak: ${stønadsendring.sak.verdi}", stønadsendring.innkreving, stønadsendring.beslutning)) {
            val hendelse = Hendelse(
                type = stønadsendring.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(stønadsendring.kravhaver).verdi,
                opprinneligKravhaverIdent = stønadsendring.kravhaver.verdi,
                skyldnerIdent = identUtils.hentNyesteIdent(stønadsendring.skyldner).verdi,
                opprinneligSkyldnerIdent = stønadsendring.skyldner.verdi,
                mottakerIdent = identUtils.hentNyesteIdent(stønadsendring.mottaker).verdi,
                opprinneligMottakerIdent = stønadsendring.mottaker.verdi,
                sakId = stønadsendring.sak.verdi,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakstidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                enhetsnummer = vedtakHendelse.enhetsnummer,
                eksternReferanse = stønadsendring.eksternReferanse,
                utsattTilDato = vedtakHendelse.innkrevingUtsattTilDato,
                omgjørVedtakId = stønadsendring.omgjørVedtakId,
                periodeListe = mapPeriodelisteTilDomene(stønadsendring.periodeListe),
            )
            return oppdragService.lagreHendelse(hendelse)
        }
        return null
    }

    private fun erInnkrevingOgEndring(hendelsestype: String, innkreving: Innkrevingstype, beslutningstype: Beslutningstype): Boolean {
        val erInnkrevingOgEndring = innkreving == Innkrevingstype.MED_INNKREVING && beslutningstype == Beslutningstype.ENDRING
        return if (erInnkrevingOgEndring) {
            secureLogger.debug { "$hendelsestype har innkreving og er en endring. Lagrer hendelse." }
            true
        } else {
            secureLogger.info { "$hendelsestype har ikke innkreving eller er ikke en endring. Lagrer ikke.." }
            false
        }
    }

    private fun mapPeriodelisteTilDomene(periodeListe: List<no.nav.bidrag.transport.behandling.vedtak.Periode>): List<Periode> = periodeListe.map { periode ->
        Periode(
            beløp = periode.beløp,
            valutakode = periode.valutakode,
            periodeFomDato = periode.periode.toDatoperiode().fom,
            periodeTilDato = periode.periode.toDatoperiode().til,
            delytelsesId = periode.delytelseId?.let { Integer.valueOf(it) },
        )
    }

    private fun opprettOppdragForEngangsbeløp(vedtakHendelse: VedtakHendelse, engangsbeløp: Engangsbeløp): Int? {
        LOGGER.debug { "Lagrer hendelse for engangsbeløp." }

        if (erInnkrevingOgEndring("Engangsbeløp (vedtakId: ${vedtakHendelse.id}, sak: ${engangsbeløp.sak.verdi}", engangsbeløp.innkreving, engangsbeløp.beslutning)) {
            val betaltBeløp = engangsbeløp.betaltBeløp ?: BigDecimal.ZERO
            val hendelse = Hendelse(
                type = engangsbeløp.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(engangsbeløp.kravhaver).verdi,
                opprinneligKravhaverIdent = engangsbeløp.kravhaver.verdi,
                skyldnerIdent = identUtils.hentNyesteIdent(engangsbeløp.skyldner).verdi,
                opprinneligSkyldnerIdent = engangsbeløp.skyldner.verdi,
                mottakerIdent = identUtils.hentNyesteIdent(engangsbeløp.mottaker).verdi,
                opprinneligMottakerIdent = engangsbeløp.mottaker.verdi,
                sakId = engangsbeløp.sak.verdi,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakstidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                enhetsnummer = vedtakHendelse.enhetsnummer,
                eksternReferanse = engangsbeløp.eksternReferanse,
                utsattTilDato = vedtakHendelse.innkrevingUtsattTilDato,
                referanse = engangsbeløp.referanse,
                omgjørVedtakId = engangsbeløp.omgjørVedtakId,
                periodeListe = listOf(
                    Periode(
                        periodeFomDato = vedtakHendelse.vedtakstidspunkt.toLocalDate().withDayOfMonth(1),
                        periodeTilDato = vedtakHendelse.vedtakstidspunkt.toLocalDate().withDayOfMonth(1).plusMonths(1),
                        beløp = engangsbeløp.beløp?.let { beløp ->
                            maxOf(beløp - betaltBeløp, BigDecimal.ZERO)
                        },
                        valutakode = engangsbeløp.valutakode,
                        delytelsesId = engangsbeløp.delytelseId?.let { Integer.valueOf(it) },
                    ),
                ),
            )
            return oppdragService.lagreHendelse(hendelse, true)
        }
        return null
    }

    fun sendKrav(oppdragIdListe: List<Int>) {
        if (harAktiveDriftAvvik()) {
            LOGGER.info { "Det finnes aktive driftsavvik. Starter derfor ikke overføring av konteringer for oppdrag: $oppdragIdListe." }
            return
        } else if (erVedlikeholdsmodusPåslått()) {
            LOGGER.info { "Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av kontering for oppdrag: $oppdragIdListe." }
            return
        }
        kravService.sendKrav(oppdragIdListe)
    }

    private fun erVedlikeholdsmodusPåslått(): Boolean = kravService.erVedlikeholdsmodusPåslått()

    private fun harAktiveDriftAvvik(erInnlesning: Boolean = false): Boolean = persistenceService.harAktivtDriftsavvik(erInnlesning)
}
