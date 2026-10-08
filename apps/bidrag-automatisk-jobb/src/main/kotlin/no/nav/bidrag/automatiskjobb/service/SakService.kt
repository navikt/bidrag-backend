package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.UnleashFeatures
import no.nav.bidrag.automatiskjobb.utils.hentSisteLøpendePeriode
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettEngangsbeløpRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.BarnISak
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService as BeregnVedtakService

private val LOGGER = KotlinLogging.logger { }

@Service
class SakService(
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val identUtils: IdentUtils,
    private val beregnVedtakService: BeregnVedtakService,
) {
    fun behandleSakHendelse(
        hendelse: SakHendelse,
        hendelseTidspunkt: Instant,
    ) {
        if (!UnleashFeatures.FATTE_ENDRING_MOTTAKER_VEDTAK.isEnabled) {
            LOGGER.info {
                "Feature toggle ${UnleashFeatures.FATTE_ENDRING_MOTTAKER_VEDTAK.featureName} er avskrudd. " +
                    "Behandler ikke sakhendelse for endring av mottaker."
            }
            return
        }
        val engangsbeløpForSak = bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(hendelse.saksnummer)
        hendelse.barn.forEach { barnISak ->
            val kravhaver = barnISak.ident?.nyesteIdent() ?: return@forEach
            val utledetMottaker = barnISak.utledMottaker(hendelse) ?: run {
                LOGGER.warn { "Kan ikke utlede mottaker for sak ${hendelse.saksnummer.verdi}. Fatter ikke vedtak." }
                return@forEach
            }
            if (utledetMottaker.erSamhandlerId()) return@forEach
            if (!utledetMottaker.erPersonIdent()) {
                LOGGER.warn { "Utledet mottaker i sak ${hendelse.saksnummer.verdi} er ikke en gyldig personident. Fatter ikke vedtak." }
                return@forEach
            }
            val nyMottaker = Personident(utledetMottaker.verdi).nyesteIdent()
            val stønadsendringer = STØNADSTYPER.mapNotNull { type ->
                behandleMottakerForStønad(hendelse, kravhaver, nyMottaker, type)
            }
            val engangsbeløp = behandleMottakerForEngangsbeløp(hendelse, kravhaver, nyMottaker, engangsbeløpForSak)
            if (stønadsendringer.isNotEmpty() || engangsbeløp.isNotEmpty()) {
                fattEndreMottakerVedtak(hendelse, hendelseTidspunkt, kravhaver, nyMottaker, stønadsendringer, engangsbeløp)
            }
        }
    }

    private fun behandleMottakerForStønad(
        hendelse: SakHendelse,
        kravhaver: Personident,
        nyMottaker: Personident,
        stønadstype: Stønadstype,
    ): OpprettStønadsendringRequestDto? {
        val skyldner = stønadstype.skyldner(hendelse) ?: return null

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
            return null
        }

        if (løpendeStønad.mottaker.nyesteIdent().verdi == nyMottaker.verdi) {
            LOGGER.info {
                "Mottaker for ${stønadstype.name.lowercase()} i sak ${stønadsid.sak.verdi} er uendret. Fatter ikke vedtak."
            }
            return null
        }

        return OpprettStønadsendringRequestDto(
            type = stønadsid.type,
            sak = stønadsid.sak,
            kravhaver = stønadsid.kravhaver,
            skyldner = stønadsid.skyldner,
            mottaker = nyMottaker,
            beslutning = Beslutningstype.ENDRING,
            innkreving = løpendeStønad.innkreving,
            sisteVedtaksid = beregnVedtakService.finnSisteVedtaksid(stønadsid),
            grunnlagReferanseListe = emptyList(),
            periodeListe = emptyList(),
        )
    }

    private fun behandleMottakerForEngangsbeløp(
        hendelse: SakHendelse,
        kravhaver: Personident,
        nyMottaker: Personident,
        engangsbeløpForSak: List<EngangsbeløpDto>,
    ): List<OpprettEngangsbeløpRequestDto> = engangsbeløpForSak
        .filter { it.type == Engangsbeløptype.SÆRBIDRAG && it.kravhaver.nyesteIdent() == kravhaver }
        .mapNotNull { kandidat ->
            val eksisterende = checkNotNull(
                bidragBeløpshistorikkConsumer.hentEngangsbeløp(
                    HentEngangsbeløpRequest(
                        type = kandidat.type,
                        sak = hendelse.saksnummer,
                        skyldner = kandidat.skyldner,
                        kravhaver = kandidat.kravhaver,
                        referanse = kandidat.referanse,
                    ),
                ),
            ) {
                "Fant ikke særbidrag fra sakslisten i sak ${hendelse.saksnummer.verdi}"
            }
            if (eksisterende.mottaker.nyesteIdent() == nyMottaker) return@mapNotNull null

            OpprettEngangsbeløpRequestDto(
                type = eksisterende.type,
                sak = eksisterende.sak,
                kravhaver = eksisterende.kravhaver,
                skyldner = eksisterende.skyldner,
                mottaker = nyMottaker,
                beslutning = Beslutningstype.ENDRING,
                innkreving = eksisterende.innkreving,
                omgjørVedtakId = eksisterende.vedtaksid,
                referanse = eksisterende.referanse,
                resultatkode = eksisterende.resultatkode,
                beløp = eksisterende.beløp,
                betaltBeløp = eksisterende.betaltBeløp,
                valutakode = eksisterende.valutakode,
                grunnlagReferanseListe = emptyList(),
            )
        }

    private fun fattEndreMottakerVedtak(
        hendelse: SakHendelse,
        hendelseTidspunkt: Instant,
        kravhaver: Personident,
        nyMottaker: Personident,
        stønadsendringer: List<OpprettStønadsendringRequestDto>,
        engangsbeløp: List<OpprettEngangsbeløpRequestDto>,
    ) {
        val request =
            OpprettVedtakRequestDto(
                type = Vedtakstype.ENDRING_MOTTAKER,
                kilde = Vedtakskilde.AUTOMATISK,
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer(ENHET_AUTOMATISK),
                unikReferanse = unikReferanse(hendelse, hendelseTidspunkt, kravhaver, nyMottaker),
                grunnlagListe = emptyList(),
                engangsbeløpListe = engangsbeløp,
                behandlingsreferanseListe = emptyList(),
                stønadsendringListe = stønadsendringer,
            )

        val vedtaksid =
            try {
                bidragVedtakConsumer.opprettVedtak(request).vedtaksid
            } catch (e: HttpStatusCodeException) {
                if (e.statusCode == HttpStatus.CONFLICT) {
                    val eksisterende = e.getResponseBodyAs(OpprettVedtakConflictResponse::class.java)!!
                    LOGGER.error {
                        "Vedtak for endring av mottaker i sak ${hendelse.saksnummer.verdi} " +
                            "finnes allerede med vedtaksid ${eksisterende.vedtaksid}. Fatter ikke nytt vedtak."
                    }
                    eksisterende.vedtaksid
                } else {
                    throw e
                }
            }

        secureLogger.info { "Endring av mottaker for vedtak $vedtaksid i sak ${hendelse.saksnummer.verdi}." }
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
        hendelse: SakHendelse,
        hendelseTidspunkt: Instant,
        kravhaver: Personident,
        nyMottaker: Personident,
    ): String {
        val tidspunkt = KOMPAKT_HENDELSE_TIDSPUNKT.format(hendelseTidspunkt)
        return "endring_mottaker_${hendelse.saksnummer.verdi}_${tidspunkt}_${hendelse.hendelsestype.name}_" +
            "${kravhaver.verdi}_${nyMottaker.verdi}"
    }

    private fun Personident.nyesteIdent(): Personident = identUtils.hentNyesteIdent(this)

    companion object {
        private const val ENHET_AUTOMATISK = "9999"
        private val STØNADSTYPER = listOf(Stønadstype.BIDRAG, Stønadstype.FORSKUDD, Stønadstype.BIDRAG18AAR)
        private val KOMPAKT_HENDELSE_TIDSPUNKT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC)
    }
}
