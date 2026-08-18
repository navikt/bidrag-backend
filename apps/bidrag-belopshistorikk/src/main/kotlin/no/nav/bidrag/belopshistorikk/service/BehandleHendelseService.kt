package no.nav.bidrag.belopshistorikk.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettEngangsbeløpRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.vedtak.Engangsbeløp
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.felles.tilJsonString
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleHendelseService(private val beløpshistorikkService: BeløpshistorikkService, private val persistenceService: PersistenceService) {

    @Transactional
    fun behandleHendelse(vedtakHendelse: VedtakHendelse) {
        LOGGER.info { "Behandler vedtakHendelse med id ${vedtakHendelse.id}" }
        secureLogger.debug { "Behandler vedtakHendelse: ${tilJsonString(vedtakHendelse)}" }

        vedtakHendelse.stønadsendringListe?.forEach { stønadsendring ->
            behandleVedtakHendelseStønadsendring(
                stønadsendring = stønadsendring,
                vedtakType = vedtakHendelse.type,
                vedtaksid = vedtakHendelse.id,
                opprettetAv = vedtakHendelse.opprettetAv,
                vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
            )
        }

        vedtakHendelse.engangsbeløpListe?.forEach { engangsbeløp ->
            behandleVedtakHendelseEngangsbeløp(
                engangsbeløp = engangsbeløp,
                vedtakType = vedtakHendelse.type,
                vedtaksid = vedtakHendelse.id,
                opprettetAv = vedtakHendelse.opprettetAv,
                vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
            )
        }
    }

    private fun behandleVedtakHendelseStønadsendring(
        stønadsendring: Stønadsendring,
        vedtakType: Vedtakstype,
        vedtaksid: Int,
        opprettetAv: String,
        vedtakstidspunkt: LocalDateTime,
    ) {
        // Sjekker om stønad skal oppdateres
        if (stønadsendring.beslutning == Beslutningstype.ENDRING && stønadsendring.innkreving == Innkrevingstype.MED_INNKREVING) {
            LOGGER.info { "Behandler stønadsendring med beslutningstype ENDRING og innkrevingstype MED_INNKREVING for vedtaksid $vedtaksid" }
            secureLogger.debug {
                "Behandler stønadsendring med beslutningstype ENDRING og innkrevingstype MED_INNKREVING for vedtaksid $vedtaksid: " +
                    tilJsonString(stønadsendring)
            }
            val eksisterendeStønad =
                beløpshistorikkService.hentStønad(
                    HentStønadRequest(
                        type = stønadsendring.type,
                        sak = stønadsendring.sak,
                        skyldner = stønadsendring.skyldner,
                        kravhaver = stønadsendring.kravhaver,
                    ),
                )

            if (eksisterendeStønad != null) {
                if (vedtakType == Vedtakstype.ENDRING_MOTTAKER) {
                    // Mottatt hendelse skal oppdatere mottaker for alle stønader i stønadsendringListe. Ingen perioder skal oppdateres.
                    LOGGER.info { "Skal oppdatere mottaker på eksisterende stønad for vedtaksid $vedtaksid" }
                    secureLogger.debug {
                        "Skal oppdatere mottaker på eksisterende stønad for vedtaksid $vedtaksid: ${tilJsonString(eksisterendeStønad)}"
                    }
                    persistenceService.endreMottakerForStønad(
                        stønadsid = eksisterendeStønad.stønadsid,
                        nyMottaker = stønadsendring.mottaker.verdi,
                        opprettetAv = opprettetAv,
                    )
                } else {
                    // Mottatt hendelse skal oppdatere eksisterende stønad
                    LOGGER.info { "Skal oppdatere eksisterende stønad for vedtaksid $vedtaksid" }
                    secureLogger.debug { "Skal oppdatere eksisterende stønad for vedtaksid $vedtaksid: ${tilJsonString(eksisterendeStønad)}" }
                    endreStønad(
                        eksisterendeStønad = eksisterendeStønad,
                        stønadsendring = stønadsendring,
                        vedtaksid = vedtaksid,
                        opprettetAv = opprettetAv,
                        vedtakstidspunkt = vedtakstidspunkt,
                    )
                }
            } else {
                // Stønaden finnes ikke fra før. Hvis det er forsøkt endret mottaker for stønad som ikke finnes så skal det logges, men ikke feile.
                if (vedtakType == Vedtakstype.ENDRING_MOTTAKER) {
                    LOGGER.warn { "Mottaker forsøkt endret for stønad som ikke finnes. Vedtaksid $vedtaksid" }
                    secureLogger.warn {
                        "Mottaker forsøkt endret for stønad som ikke finnes. Vedtaksid $vedtaksid. Stønadsendring ${tilJsonString(stønadsendring)}"
                    }
                } else {
                    LOGGER.info { "Skal opprette ny stønad for vedtaksid $vedtaksid" }
                    secureLogger.debug { "Skal opprette ny stønad for vedtaksid $vedtaksid. Stønadsendring ${tilJsonString(stønadsendring)}" }
                    opprettStønad(
                        stønadsendring = stønadsendring,
                        vedtaksid = vedtaksid,
                        opprettetAv = opprettetAv,
                        vedtakstidspunkt = vedtakstidspunkt,
                    )
                }
            }
        } else {
            LOGGER.info { "Stønadsendring for vedtaksid $vedtaksid kvalifiserer ikke for videre behandling" }
            secureLogger.debug { "Stønadsendring for vedtaksid $vedtaksid kvalifiserer ikke for videre behandling: ${tilJsonString(stønadsendring)}" }
        }
    }

    private fun behandleVedtakHendelseEngangsbeløp(
        engangsbeløp: Engangsbeløp,
        vedtakType: Vedtakstype,
        vedtaksid: Int,
        opprettetAv: String,
        vedtakstidspunkt: LocalDateTime,
    ) {
        // Sjekker om engangsbeløp skal oppdateres
        if (engangsbeløp.beslutning == Beslutningstype.ENDRING && engangsbeløp.innkreving == Innkrevingstype.MED_INNKREVING) {
            LOGGER.info { "Behandler engangsbeløp med beslutningstype ENDRING og innkrevingstype MED_INNKREVING for vedtaksid $vedtaksid" }
            secureLogger.debug {
                "Behandler engangsbeløp med beslutningstype ENDRING og innkrevingstype MED_INNKREVING for vedtaksid $vedtaksid: " +
                    tilJsonString(engangsbeløp)
            }
            val eksisterendeEngangsbeløp =
                beløpshistorikkService.hentEngangsbeløp(
                    HentEngangsbeløpRequest(
                        type = engangsbeløp.type,
                        sak = engangsbeløp.sak,
                        skyldner = engangsbeløp.skyldner,
                        kravhaver = engangsbeløp.kravhaver,
                        referanse = engangsbeløp.referanse,
                    ),
                )

            if (eksisterendeEngangsbeløp != null) {
                if (vedtakType == Vedtakstype.ENDRING_MOTTAKER) {
                    // Mottatt hendelse skal oppdatere mottaker for engangsbeløp
                    LOGGER.info { "Skal oppdatere mottaker på eksisterende engangsbeløp for vedtaksid $vedtaksid" }
                    secureLogger.debug {
                        "Skal oppdatere mottaker på eksisterende engangsbeløp for vedtaksid $vedtaksid: ${tilJsonString(eksisterendeEngangsbeløp)}"
                    }
                    persistenceService.endreMottakerForEngangsbeløp(
                        engangsbeløpsid = eksisterendeEngangsbeløp.engangsbeløpsid,
                        nyMottaker = engangsbeløp.mottaker.verdi,
                        opprettetAv = opprettetAv,
                    )
                } else {
                    // Mottatt hendelse skal oppdatere eksisterende engangsbeløp
                    LOGGER.info { "Skal oppdatere eksisterende engangsbeløp for vedtaksid $vedtaksid" }
                    secureLogger.debug {
                        "Skal oppdatere eksisterende engangsbeløp for vedtaksid $vedtaksid: ${tilJsonString(eksisterendeEngangsbeløp)}"
                    }
                    endreEngangsbeløp(
                        eksisterendeEngangsbeløp = eksisterendeEngangsbeløp,
                        engangsbeløp = engangsbeløp,
                        vedtaksid = vedtaksid,
                        opprettetAv = opprettetAv,
                        vedtakstidspunkt = vedtakstidspunkt,
                    )
                }
            } else {
                // Stønaden finnes ikke fra før. Hvis det er forsøkt endret mottaker for engangsbeløp som ikke finnes så skal det logges, men ikke feile.
                if (vedtakType == Vedtakstype.ENDRING_MOTTAKER) {
                    LOGGER.warn { "Mottaker forsøkt endret for engangsbeløp som ikke finnes. Vedtaksid $vedtaksid" }
                    secureLogger.warn {
                        "Mottaker forsøkt endret for engangsbeløp som ikke finnes. Vedtaksid $vedtaksid. Engangsbeløp ${tilJsonString(engangsbeløp)}"
                    }
                } else {
                    if (engangsbeløp.beløp != null) {
                        // Kun engangsbeløp med beløp skal lagres
                        LOGGER.info { "Skal opprette nytt engangsbeløp for vedtaksid $vedtaksid" }
                        secureLogger.debug { "Skal opprette nytt engangsbeløp for vedtaksid $vedtaksid" }
                        opprettEngangsbeløp(
                            engangsbeløp = engangsbeløp,
                            vedtaksid = vedtaksid,
                            opprettetAv = opprettetAv,
                            vedtakstidspunkt = vedtakstidspunkt,
                        )
                    } else {
                        // Ingen engangsbeløp med beløp = null skal lagres
                        LOGGER.info { "Engangsbeløp for vedtaksid $vedtaksid er null og vil ikke bli lagret" }
                        secureLogger.debug { "Engangsbeløp for vedtaksid $vedtaksid er null og vil ikke bli lagret: ${tilJsonString(engangsbeløp)}" }
                    }
                }
            }
        } else {
            LOGGER.info { "Engangsbeløp for vedtaksid $vedtaksid kvalifiserer ikke for videre behandling" }
            secureLogger.debug { "Engangsbeløp for vedtaksid $vedtaksid kvalifiserer ikke for videre behandling: ${tilJsonString(engangsbeløp)}" }
        }
    }

    private fun endreStønad(
        eksisterendeStønad: StønadDto,
        stønadsendring: Stønadsendring,
        vedtaksid: Int,
        opprettetAv: String,
        vedtakstidspunkt: LocalDateTime,
    ) {
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val hendelsePeriodeListe = stønadsendring.periodeListe.sortedBy { it.periode.fom }
        var i = 1
        hendelsePeriodeListe.forEach {
            periodeListe.add(
                OpprettStønadsperiodeRequestDto(
                    periode = ÅrMånedsperiode(
                        fom = it.periode.fom,
                        til = finnPeriodeTil(
                            til = it.periode.til,
                            periodeListe = hendelsePeriodeListe,
                            i = i,
                        ),
                    ),
                    vedtaksid = vedtaksid,
                    gyldigFra = vedtakstidspunkt,
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = null,
                    beløp = it.beløp,
                    valutakode = it.valutakode,
                    resultatkode = it.resultatkode,
                ),
            )
            i++
        }

        val oppdatertStønad =
            OpprettStønadRequestDto(
                type = stønadsendring.type,
                sak = stønadsendring.sak,
                skyldner = stønadsendring.skyldner,
                kravhaver = stønadsendring.kravhaver,
                mottaker = stønadsendring.mottaker,
                nesteIndeksreguleringsår = stønadsendring.førsteIndeksreguleringsår,
                innkreving = stønadsendring.innkreving,
                opprettetAv = opprettetAv,
                periodeListe = periodeListe,
            )

        beløpshistorikkService.endreStønad(eksisterendeStønad, oppdatertStønad, vedtakstidspunkt)
    }

    private fun endreEngangsbeløp(
        eksisterendeEngangsbeløp: EngangsbeløpDto,
        engangsbeløp: Engangsbeløp,
        vedtaksid: Int,
        opprettetAv: String,
        vedtakstidspunkt: LocalDateTime,
    ) {
        val oppdatertEngangsbeløp =
            OpprettEngangsbeløpRequestDto(
                type = engangsbeløp.type,
                sak = engangsbeløp.sak,
                skyldner = engangsbeløp.skyldner,
                kravhaver = engangsbeløp.kravhaver,
                mottaker = engangsbeløp.mottaker,
                vedtaksid = vedtaksid,
                gyldigFra = vedtakstidspunkt,
                gyldigTil = null,
                gjortUgyldigAvVedtaksid = null,
                beløp = engangsbeløp.beløp,
                betaltBeløp = engangsbeløp.betaltBeløp,
                valutakode = engangsbeløp.valutakode,
                resultatkode = engangsbeløp.resultatkode,
                innkreving = engangsbeløp.innkreving,
                referanse = engangsbeløp.referanse,
                opprettetAv = opprettetAv,
            )

        beløpshistorikkService.endreEngangsbeløp(
            eksisterendeEngangsbeløp = eksisterendeEngangsbeløp,
            oppdatertEngangsbeløp = oppdatertEngangsbeløp,
            vedtaksid = vedtaksid,
            vedtakstidspunkt = vedtakstidspunkt,
        )
    }

    private fun opprettStønad(stønadsendring: Stønadsendring, vedtaksid: Int, opprettetAv: String, vedtakstidspunkt: LocalDateTime) {
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val hendelsePeriodeListe = stønadsendring.periodeListe.sortedBy { it.periode.fom }
        var i = 1
        hendelsePeriodeListe.forEach { periode ->
            // Kun perioder med beløp skal lagres
            if (periode.beløp != null) {
                periodeListe.add(
                    OpprettStønadsperiodeRequestDto(
                        periode = ÅrMånedsperiode(
                            fom = periode.periode.fom,
                            til = finnPeriodeTil(
                                til = periode.periode.til,
                                periodeListe = hendelsePeriodeListe,
                                i = i,
                            ),
                        ),
                        vedtaksid = vedtaksid,
                        gyldigFra = vedtakstidspunkt,
                        gyldigTil = null,
                        periodeGjortUgyldigAvVedtaksid = null,
                        beløp = periode.beløp,
                        valutakode = periode.valutakode,
                        resultatkode = periode.resultatkode,
                    ),
                )
            }
            i++
        }

        // Hvis periodelisten er tom (kun perioder med beløp = null) så skal stønaden ikke opprettes
        if (periodeListe.isNotEmpty()) {
            beløpshistorikkService.opprettStønad(
                OpprettStønadRequestDto(
                    type = stønadsendring.type,
                    sak = stønadsendring.sak,
                    skyldner = stønadsendring.skyldner,
                    kravhaver = stønadsendring.kravhaver,
                    mottaker = stønadsendring.mottaker,
                    nesteIndeksreguleringsår = stønadsendring.førsteIndeksreguleringsår,
                    innkreving = stønadsendring.innkreving,
                    opprettetAv = opprettetAv,
                    periodeListe = periodeListe,
                ),
            )
        } else {
            LOGGER.warn { "Periodelisten er tom (alle beløp er null). Stønad vil ikke bli opprettet for vedtaksid $vedtaksid" }
            secureLogger.warn {
                "Periodelisten er tom (alle beløp er null). Stønad vil ikke bli opprettet for vedtaksid $vedtaksid. " +
                    "Stønadsendring ${tilJsonString(stønadsendring)}"
            }
        }
    }

    private fun opprettEngangsbeløp(engangsbeløp: Engangsbeløp, vedtaksid: Int, opprettetAv: String, vedtakstidspunkt: LocalDateTime) {
        beløpshistorikkService.opprettEngangsbeløp(
            OpprettEngangsbeløpRequestDto(
                type = engangsbeløp.type,
                sak = engangsbeløp.sak,
                skyldner = engangsbeløp.skyldner,
                kravhaver = engangsbeløp.kravhaver,
                mottaker = engangsbeløp.mottaker,
                vedtaksid = vedtaksid,
                gyldigFra = vedtakstidspunkt,
                gyldigTil = null,
                gjortUgyldigAvVedtaksid = null,
                beløp = engangsbeløp.beløp,
                betaltBeløp = engangsbeløp.betaltBeløp,
                valutakode = engangsbeløp.valutakode,
                resultatkode = engangsbeløp.resultatkode,
                innkreving = engangsbeløp.innkreving,
                referanse = engangsbeløp.referanse,
                opprettetAv = opprettetAv,
            ),
        )
    }

    private fun finnPeriodeTil(til: YearMonth?, periodeListe: List<Periode>, i: Int): YearMonth? = if (i == periodeListe.size) {
        // Siste element i listen, til skal ikke justeres
        til
    } else {
        til ?: periodeListe[i].periode.fom
    }
}
