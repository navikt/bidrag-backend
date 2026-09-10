package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.utils.hentSisteLøpendePeriode
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.BarnISak
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService as BeregnVedtakService

private val LOGGER = KotlinLogging.logger { }

@Service
class SakService(
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val identUtils: IdentUtils,
    private val beregnVedtakService: BeregnVedtakService,
) {
    fun behandleSakHendelse(hendelse: SakHendelse) {
        hendelse.barn.forEach { barnISak ->
            STØNADSTYPER.forEach { stønadstype ->
                behandleMottakerForStønad(hendelse, barnISak, stønadstype)
            }
        }
    }

    private fun behandleMottakerForStønad(
        hendelse: SakHendelse,
        barnISak: BarnISak,
        stønadstype: Stønadstype,
    ) {
        val kravhaver = barnISak.ident?.nyesteIdent() ?: return
        val skyldner = stønadstype.skyldner(hendelse) ?: return

        val stønadsid =
            Stønadsid(
                type = stønadstype,
                kravhaver = kravhaver,
                skyldner = skyldner,
                sak = hendelse.saksnummer,
            )

        val løpendeStønad =
            bidragBeløpshistorikkConsumer.hentLøpendeStønad(
                HentStønadRequest(
                    type = stønadsid.type,
                    sak = stønadsid.sak,
                    skyldner = stønadsid.skyldner,
                    kravhaver = stønadsid.kravhaver,
                ),
            )

        val løpendePeriode = løpendeStønad?.periodeListe?.hentSisteLøpendePeriode()
        if (løpendeStønad == null || løpendePeriode == null) {
            LOGGER.info {
                "Ingen løpende ${stønadstype.name.lowercase()} for sak ${stønadsid.sak.verdi}. Ingen mottakerendring å utlede."
            }
            return
        }

        val utledetMottaker = barnISak.utledMottaker(hendelse)
        if (utledetMottaker == null) {
            LOGGER.warn {
                "Kan ikke utlede mottaker for ${stønadstype.name.lowercase()} i sak ${stønadsid.sak.verdi}. Fatter ikke vedtak."
            }
            return
        }
        if (utledetMottaker.erSamhandlerId()) {
            return
        }
        if (!utledetMottaker.erPersonIdent()) {
            LOGGER.warn {
                "Utledet mottaker for ${stønadstype.name.lowercase()} i sak ${stønadsid.sak.verdi} er ikke en gyldig personident. " +
                    "Fatter ikke vedtak."
            }
            return
        }

        val nyMottaker = Personident(utledetMottaker.verdi).nyesteIdent()

        if (løpendeStønad.mottaker.nyesteIdent().verdi == nyMottaker.verdi) {
            LOGGER.info {
                "Mottaker for ${stønadstype.name.lowercase()} i sak ${stønadsid.sak.verdi} er uendret. Fatter ikke vedtak."
            }
            return
        }

        fattEndreMottakerVedtak(stønadsid, løpendeStønad, nyMottaker)
    }

    private fun fattEndreMottakerVedtak(
        stønadsid: Stønadsid,
        løpendeStønad: StønadDto,
        nyMottaker: Personident,
    ) {
        val respons =
            bidragVedtakConsumer.opprettVedtak(
                OpprettVedtakRequestDto(
                    type = Vedtakstype.ENDRING_MOTTAKER,
                    kilde = Vedtakskilde.AUTOMATISK,
                    vedtakstidspunkt = LocalDateTime.now(),
                    enhetsnummer = Enhetsnummer(ENHET_AUTOMATISK),
                    unikReferanse = unikReferanse(stønadsid, nyMottaker),
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
                            innkreving = løpendeStønad.innkreving,
                            sisteVedtaksid = beregnVedtakService.finnSisteVedtaksid(stønadsid),
                            grunnlagReferanseListe = emptyList(),
                            periodeListe = emptyList()
                        ),
                    ),
                ),
            )
        LOGGER.info {
            "Fattet vedtak ${respons.vedtaksid} for endring av mottaker for ${stønadsid.type.name.lowercase()} " +
                "i sak ${stønadsid.sak.verdi}."
        }
        secureLogger.info { "Endring av mottaker for ${stønadsid.toReferanse()}, ny mottaker $nyMottaker." }
    }

    private fun BarnISak.utledMottaker(hendelse: SakHendelse): Ident? = if (reellMottaker != null) {
        Ident(reellMottaker!!.verdi)
    } else {
        hendelse.bidragsmottaker?.let { Ident(it.verdi) }
    }

    private fun Stønadstype.skyldner(hendelse: SakHendelse): Personident? = when (this) {
        Stønadstype.FORSKUDD -> personidentNav
        else -> hendelse.bidragspliktig?.nyesteIdent()
    }

    private fun unikReferanse(
        stønadsid: Stønadsid,
        nyMottaker: Personident,
    ) = "endring_mottaker_${stønadsid.toReferanse()}_${nyMottaker.verdi}"

    private fun Personident.nyesteIdent(): Personident = identUtils.hentNyesteIdent(this)

    companion object {
        private const val ENHET_AUTOMATISK = "9999"
        private val STØNADSTYPER = listOf(Stønadstype.BIDRAG, Stønadstype.FORSKUDD, Stønadstype.BIDRAG18AAR)
    }
}
