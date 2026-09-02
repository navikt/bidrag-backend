package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Sak
import no.nav.bidrag.automatiskjobb.persistence.entity.SakBarn
import no.nav.bidrag.automatiskjobb.persistence.repository.SakRepository
import no.nav.bidrag.automatiskjobb.utils.hentSisteLøpendePeriode
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService as BeregnVedtakService

private val LOGGER = KotlinLogging.logger { }

@Service
class SakService(
    private val sakRepository: SakRepository,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val identUtils: IdentUtils,
    private val beregnVedtakService: BeregnVedtakService,
) {
    @Transactional
    fun behandleSakHendelse(hendelse: SakHendelse) {
        val saksnummer = hendelse.saksnummer.verdi
        val lagretSak = sakRepository.findBySaksnummer(saksnummer)

        if (lagretSak == null) {
            sakRepository.save(hendelse.tilNyttSnapshot())
            LOGGER.info { "Lagret første snapshot for sak $saksnummer. Ingen endringer å utlede." }
            return
        }

        val mottakerendringer = oppdaterSnapshot(lagretSak, hendelse)
        mottakerendringer.forEach { opprettVedtaksforslagForEndretMottakerForskudd(hendelse, it) }
    }

    private fun oppdaterSnapshot(
        lagretSak: Sak,
        hendelse: SakHendelse,
    ): List<Mottakerendring> {
        val mottakerendringer = mutableListOf<Mottakerendring>()

        lagretSak.bidragspliktig = hendelse.bidragspliktig?.nyesteIdent()
        lagretSak.bidragsmottaker = hendelse.bidragsmottaker?.nyesteIdent()

        hendelse.barn.forEach { barnISak ->
            val kravhaver = barnISak.ident?.nyesteIdent() ?: return@forEach
            val nyReellMottaker = barnISak.reellMottaker?.nyesteIdent()
            val lagretBarn = lagretSak.finnBarn(kravhaver)

            if (lagretBarn == null) {
                lagretSak.barn.add(SakBarn(sak = lagretSak, kravhaver = kravhaver, reellMottaker = nyReellMottaker))
                return@forEach
            }

            if (lagretBarn.reellMottaker.normalisertReellMottaker() != nyReellMottaker) {
                mottakerendringer.add(Mottakerendring(kravhaver, nyReellMottaker))
                lagretBarn.reellMottaker = nyReellMottaker
                lagretBarn.endretTidspunkt = LocalDateTime.now()
            }
        }

        lagretSak.endretTidspunkt = LocalDateTime.now()
        sakRepository.save(lagretSak)
        return mottakerendringer
    }

    private fun opprettVedtaksforslagForEndretMottakerForskudd(
        hendelse: SakHendelse,
        endring: Mottakerendring,
    ) {
        val stønadsid =
            Stønadsid(
                type = Stønadstype.FORSKUDD,
                kravhaver = Personident(endring.kravhaver),
                skyldner = personidentNav,
                sak = hendelse.saksnummer,
            )

        val løpendePeriode =
            bidragBeløpshistorikkConsumer
                .hentLøpendeStønad(
                    HentStønadRequest(
                        type = stønadsid.type,
                        sak = stønadsid.sak,
                        skyldner = stønadsid.skyldner,
                        kravhaver = stønadsid.kravhaver,
                    ),
                )?.periodeListe
                ?.hentSisteLøpendePeriode()

        if (løpendePeriode == null) {
            LOGGER.info {
                "Ingen løpende forskudd for sak ${stønadsid.sak.verdi}. Oppretter ikke vedtaksforslag for endring av mottaker."
            }
            return
        }

        val nyMottaker = endring.nyMottakerIdent(hendelse)
        if (nyMottaker == null) {
            LOGGER.warn {
                "Kan ikke utlede ny mottaker for forskudd i sak ${stønadsid.sak.verdi} " +
                    "(reell mottaker er trolig en samhandler). Oppretter ikke vedtaksforslag."
            }
            return
        }

        bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(
            OpprettVedtakRequestDto(
                type = Vedtakstype.ENDRING_MOTTAKER,
                kilde = Vedtakskilde.AUTOMATISK,
                vedtakstidspunkt = null,
                enhetsnummer = Enhetsnummer(ENHET_AUTOMATISK),
                unikReferanse = endring.unikReferanse(stønadsid),
                grunnlagListe = emptyList(),
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                stønadsendringListe =
                listOf(
                    OpprettStønadsendringRequestDto(
                        type = stønadsid.type,
                        sak = stønadsid.sak,
                        kravhaver = stønadsid.kravhaver,
                        skyldner = stønadsid.skyldner,
                        mottaker = nyMottaker,
                        beslutning = Beslutningstype.ENDRING,
                        innkreving = Innkrevingstype.MED_INNKREVING,
                        sisteVedtaksid = beregnVedtakService.finnSisteVedtaksid(stønadsid),
                        grunnlagReferanseListe = emptyList(),
                        periodeListe =
                        listOf(
                            OpprettPeriodeRequestDto(
                                periode = løpendePeriode.periode,
                                beløp = løpendePeriode.beløp,
                                valutakode = løpendePeriode.valutakode,
                                resultatkode = løpendePeriode.resultatkode,
                                grunnlagReferanseListe = emptyList(),
                            ),
                        ),
                    ),
                ),
            ),
        )
        LOGGER.info { "Opprettet vedtaksforslag for endring av mottaker for forskudd i sak ${stønadsid.sak.verdi}." }
        secureLogger.info { "Endring av mottaker for forskudd: ${stønadsid.toReferanse()}, ny mottaker $nyMottaker." }
    }

    private fun SakHendelse.tilNyttSnapshot(): Sak {
        val sak =
            Sak(
                saksnummer = saksnummer.verdi,
                bidragspliktig = bidragspliktig?.nyesteIdent(),
                bidragsmottaker = bidragsmottaker?.nyesteIdent(),
            )
        sak.barn.addAll(
            barn.mapNotNull { barnISak ->
                val kravhaver = barnISak.ident?.nyesteIdent() ?: return@mapNotNull null
                SakBarn(
                    sak = sak,
                    kravhaver = kravhaver,
                    reellMottaker = barnISak.reellMottaker?.nyesteIdent(),
                )
            },
        )
        return sak
    }

    private fun Personident.nyesteIdent(): String = identUtils.hentNyesteIdent(this).verdi

    private fun ReellMottaker.nyesteIdent(): String = personIdent()?.let { identUtils.hentNyesteIdent(it).verdi } ?: verdi

    private fun String?.normalisertReellMottaker(): String? = this?.let { ReellMottaker(it).nyesteIdent() }

    private data class Mottakerendring(
        val kravhaver: String,
        val nyReellMottaker: String?,
    ) {
        /**
         * Når reell mottaker fjernes, går utbetalingen tilbake til bidragsmottaker.
         * En samhandler-id kan ikke brukes som mottaker på en stønadsendring, og gir derfor ingen ident.
         */
        fun nyMottakerIdent(hendelse: SakHendelse): Personident? = if (nyReellMottaker != null) {
            ReellMottaker(nyReellMottaker).personIdent()
        } else {
            hendelse.bidragsmottaker
        }

        fun unikReferanse(stønadsid: Stønadsid) = "endring_mottaker_${stønadsid.toReferanse()}_${nyReellMottaker ?: "bm"}"
    }

    companion object {
        private const val ENHET_AUTOMATISK = "9999"
    }
}
