package no.nav.bidrag.bbm.service

import no.nav.bidrag.bbm.bo.FinnSammenknytningerHovedsøknadRequest
import no.nav.bidrag.bbm.bo.FinnSammenknytningerHovedsøknadResponse
import no.nav.bidrag.bbm.bo.Gebyrsøknad
import no.nav.bidrag.bbm.bo.SammenknyttSøknaderRequest
import no.nav.bidrag.bbm.bo.SlettHovedsøknadRequest
import no.nav.bidrag.bbm.bo.SlettSammenknytningForSøknadRequest
import no.nav.bidrag.bbm.bo.SøknadsknytningStatus
import no.nav.bidrag.bbm.exception.mismatchEksisterendeBehandlingsid
import no.nav.bidrag.bbm.exception.søknadFinnesIkke
import no.nav.bidrag.bbm.model.erForholdsmessigFordeling
import no.nav.bidrag.bbm.persistence.bisys.entity.Blankett
import no.nav.bidrag.bbm.persistence.bisys.entity.Hendelse
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknad
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadsknytning
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadslinje
import no.nav.bidrag.bbm.persistence.bisys.repository.BlankettRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.HendelseRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.RolleRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadsknytningRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadslinjeRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.VedtakOverføringRepository
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadsBarnRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentBPsÅpneSøknaderResponse
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.LeggTilBarnIFFSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlerenhetRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterReferanseGebyrRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

val søknadstyperFF = listOf(Behandlingstype.FORHOLDSMESSIG_FORDELING, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE)
val søknadstyperFFKoder = søknadstyperFF.map { it.bisysKode }

@Service
class BisysService(
    private val søknadRepository: SøknadRepository,
    private val vedtakOverføringRepository: VedtakOverføringRepository,
    private val søknadslinjeRepository: SøknadslinjeRepository,
    private val rolleRepository: RolleRepository,
    private val blankettRepository: BlankettRepository,
    private val hendelseRepository: HendelseRepository,
    private val søknadsknytningRepository: SøknadsknytningRepository,
) {
    fun hentÅpneSøknaderForPerson(personident: String): HentBPsÅpneSøknaderResponse {
        val saksnummerListe =
            rolleRepository.finnBPsSaker(personident)

        secureLogger.info { "Saksnummer funnet for BP: $saksnummerListe" }
        val åpneSøknaderListe = søknadRepository.finnÅpneSøknader(saksnummerListe)

        secureLogger.info { "Åpne søknader funnet for BP: $åpneSøknaderListe" }

        return HentBPsÅpneSøknaderResponse(
            åpneSøknader =
            åpneSøknaderListe
                .map { åpenSøknad ->
                    val søknadHarInnkreving =
                        søknadRepository
                            .finnTilhørendeInnkrevingssøknaderOgSøknadslinjer(
                                åpenSøknad.blankettid!!,
                            ).isNotEmpty()
                    val søknadslinjer = søknadslinjeRepository.finnSøknadslinjerForSøknad(åpenSøknad.søknadsid!!)
                    HentSøknad(
                        behandlingstype = Behandlingstype.fraKode(blankettRepository.finnSøknadstype(åpenSøknad.blankettid))!!,
                        behandlerenhet = åpenSøknad.behandlerenhet,
                        saksnummer = åpenSøknad.saksnummer,
                        søknadsid = åpenSøknad.søknadsid!!,
                        refVedtaksid = åpenSøknad.refVedtaksid,
                        refSøknadsid = åpenSøknad.refSøknadsid,
                        behandlingsid = åpenSøknad.behandlingsid?.toLong(),
                        behandlingStatusType = finnBehandlingsstatusType(søknadslinjer),
                        behandlingstema =
                        when (åpenSøknad.søknadsgruppekode) {
                            "BI" -> Behandlingstema.BIDRAG
                            "18" -> Behandlingstema.BIDRAG_18_ÅR
                            else -> Behandlingstema.OPPFOSTRINGSBIDRAG
                        },
                        innkreving = søknadHarInnkreving,
                        søknadMottattDato = åpenSøknad.søknadMottattDato,
                        søknadFomDato = åpenSøknad.søknadFomDato,
                        søktAvType = SøktAvType.fraKode(blankettRepository.finnSøknadFraKode(åpenSøknad.blankettid))!!,
                        partISøknadListe =
                        finnPartISøknad(
                            åpenSøknad.blankettid,
                            åpenSøknad.saksnummer,
                            søknadslinjer,
                        ),
                    )
                }.distinct(),
        )
    }

    private fun finnPartISøknad(
        blankettid: Long,
        saksnummer: String,
        søknadslinjer: List<Søknadslinje>,
    ): List<PartISøknad> {
        val roller = rolleRepository.finnRollerISak(saksnummer)
        val gebyrsøknader = hentGebyr(blankettid)
        val parterSøknadsbarn =
            søknadslinjer.map { søknadslinje ->
                val rolle = roller.firstOrNull { it.rolleid == søknadslinje.rolleid }!!

                PartISøknad(
                    personident = rolle.fnr,
                    rolletype = rolle.tilRolletype(),
                    behandlingstatus = Behandlingstatus.fraKode(søknadslinje.søknadStatuskode),
                    innbetaltBeløp = søknadslinje.innbetaltBeløp,
                    gebyr = gebyrsøknader.any { it.rolleid == rolle.rolleid },
                    referanseGebyr = søknadslinje.engangsbeløpReferanse,
                )
            }

        val parterBMBP =
            roller
                .filter { listOf("BP", "BM").contains(it.rolletype) }
                .map { rolle ->
                    PartISøknad(
                        personident = rolle.fnr,
                        rolletype = rolle.tilRolletype(),
                        behandlingstatus = null,
                        innbetaltBeløp = null,
                        gebyr = gebyrsøknader.any { it.rolleid == rolle.rolleid },
                        referanseGebyr = gebyrsøknader.firstOrNull { it.rolleid == rolle.rolleid }?.referanse,
                    )
                }

        return parterBMBP + parterSøknadsbarn
    }

    private fun hentGebyr(blankettid: Long): List<Gebyrsøknad> = søknadRepository
        .finnTilhørendeGebyrsøknader(blankettid)

    // Logikk for å opprette søknader
    @Transactional("bisysTransactionManager")
    fun opprettSøknader(request: OpprettSøknadRequest): OpprettSøknadResponse {
        // Kaster exception hvis det ikke ligger barn i requesten
        if (request.barnListe.isEmpty()) {
            throw HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Det må ligge minst ett barn i søknaden ved opprettelse av søknad for saksnummer: ${request.saksnummer}",
            )
        }

        val gruppeKombinasjonskodeRequest =
            SøknadGruppeKombinasjon
                .fraBehandlingstemaOgInnkreving(
                    request.behandlingstema,
                    request.innkreving,
                )!!
                .kode

        // Sjekker om det allerede finnes en søknad som matcher requesten. Hvis det er en FF-søknad så skal det sjekkes om alle søknadslinjer
        // er feilregistrerte.
        val eksisterendeSøknad =
            søknadRepository.finnEksisterendeIkkeFeilregistrertSøknad(
                saksnummer = request.saksnummer,
                behandlingsid = request.behandlingsid?.toInt(),
                behandlerenhet = request.behandlerenhet,
                refVedtaksid = request.refVedtaksid,
                søknadFomDato = request.søknadFomDato,
                søknadsgruppekode = request.behandlingstema.bisysKode,
            )

        if (eksisterendeSøknad != null) {
            secureLogger.info {
                "Fant eksisterende søknad med id ${eksisterendeSøknad.søknadsid} for saksnummer ${request.saksnummer} og behandlingsid ${request.behandlingsid}"
            }
            // Søknad finnes fra før. Sjekk om søknaden inneholder minst ett av barna i requesten. Hvis det er tilfelle så skal eksisterende
            // søknad returneres. Unntak er hvis eksisterende søknad er en forholdsmessig fordeling-søknad der alle søknadslinjer er feilregistrert,
            // da skal det opprettes en ny søknad selv om barna i requesten finnes i eksisterende søknad.
            val søknadslinjer =
                søknadslinjeRepository
                    .finnSøknadslinjerForSøknad(eksisterendeSøknad.søknadsid!!)

            // Hvis eksisterende søknad er av typen forholdsmessig fordeling så skal det sjekkes om alle søknadslinjer er feilregistrert.
            // Hvis alle søknadslinjer er feilregistrert så kan det opprettes en ny søknad, hvis ikke så skal eksisterende søknad returneres.
            val eksisterendeSøknadErFeilregistrert = søknadslinjer.all { it.søknadStatuskode == Behandlingstatus.FEILREGISTRERT.bisysKode }

            val rollerFnrMap = rolleRepository.finnRollerISak(request.saksnummer).associate { it.rolleid to it.fnr }
            val personerMedSøknadslinjer =
                søknadslinjer
                    .mapNotNull { rollerFnrMap[it.rolleid] }

            if (request.barnListe.any { barn -> personerMedSøknadslinjer.contains(barn.personident) } &&
                søknadslinjer.any { sl -> sl.gruppeKombinasjonskode == gruppeKombinasjonskodeRequest } &&
                !eksisterendeSøknadErFeilregistrert
            ) {
                return OpprettSøknadResponse(eksisterendeSøknad.søknadsid!!)
            }
        }

        // Oppretter først blankett som alle søknader skal knyttes til
        val blankett =
            try {
                val opprettBlankett =
                    Blankett(
                        saksnummer = request.saksnummer,
                        søknadFraKode = request.søktAv?.kode ?: SøktAvType.NAV_BIDRAG.kode,
                        søknadstype = request.behandlingstype?.bisysKode ?: "FF",
                        // TODO(Her bør det kastes feil om behandlingstype er null)
                    )

                blankettRepository.save(
                    opprettBlankett,
                )
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av blankett for søknad for saksnummer: ${request.saksnummer}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }

        val opprettetSøknadsid =
            when (request.behandlingstema) {
                Behandlingstema.BIDRAG -> {
                    behandleBidrag(
                        opprettSøknad = request,
                        blankettid = blankett.blankettid!!,
                    )
                }

                Behandlingstema.BIDRAG_18_ÅR -> {
                    behandleBidrag18År(
                        opprettSøknad = request,
                        blankettid = blankett.blankettid!!,
                    )
                }

                Behandlingstema.OPPFOSTRINGSBIDRAG -> {
                    behandleOppfostringsbidrag(
                        opprettSøknad = request,
                        blankettid = blankett.blankettid!!,
                    )
                }

                Behandlingstema.FORSKUDD -> {
                    if (request.behandlingstype == Behandlingstype.REVURDERING) {
                        behandleRevurderingForskudd(
                            opprettSøknad = request,
                            blankettid = blankett.blankettid!!,
                        )
                    } else {
                        throw HttpClientErrorException(
                            HttpStatus.BAD_REQUEST,
                            "Ugyldig behandlingstema og behandlingstype angitt: ${request.behandlingstema}, ${request.behandlingstype}",
                        )
                    }
                }

                else -> {
                    throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Ugyldig behandlingstema angitt: ${request.behandlingstema}")
                }
            }

        // Sjekk om hovedsøknadsid er utfylt. Hvis angitt søknad finnes, lagre i så fall kobling i søknadsknytningstabellen
        if (request.hovedsøknadsid != null) {
            søknadRepository.finnSøknad(request.hovedsøknadsid!!)
                ?: throw HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Ugyldig hovedsøknadsid angitt: ${request.hovedsøknadsid}",
                )
            val opprettetTidspunkt = LocalDateTime.now()

            val søknadsknytning =
                Søknadsknytning(
                    hovedsøknadsid = request.hovedsøknadsid,
                    referertSøknadsid = opprettetSøknadsid,
                    status = SøknadsknytningStatus.Aktiv.name,
                    søknadKnytningstype = request.behandlingstype?.bisysKode ?: "FF",
                    opprettetTidspunkt = opprettetTidspunkt,
                )
            søknadsknytningRepository.save(søknadsknytning)

            // Sjekker om det finnes en egen forekomst for hovedsøknaden. Hvis ikke så opprettes den.
            val søknadsknytningHovedsøknad =
                søknadsknytningRepository.finnSøknadsknytningReferertSøknad(
                    referertSøknadsid = request.hovedsøknadsid!!,
                    status = SøknadsknytningStatus.Aktiv.name,
                )

            if (søknadsknytningHovedsøknad.isEmpty()) {
                val søknadsknytning =
                    Søknadsknytning(
                        hovedsøknadsid = request.hovedsøknadsid,
                        referertSøknadsid = request.hovedsøknadsid,
                        status = SøknadsknytningStatus.Aktiv.name,
                        søknadKnytningstype = request.behandlingstype?.bisysKode ?: "FF",
                        opprettetTidspunkt = opprettetTidspunkt,
                    )
                søknadsknytningRepository.save(søknadsknytning)
            }
        }

        return OpprettSøknadResponse(opprettetSøknadsid)
    }

    private fun behandleBidrag(
        opprettSøknad: OpprettSøknadRequest,
        blankettid: Long,
    ): Long {
        val søknadBidrag =
            opprettOgLagreSøknad(
                opprettSøknad = opprettSøknad,
                blankettid = blankettid,
            )

        val gruppeKombinasjonskode =
            SøknadGruppeKombinasjon
                .fraBehandlingstemaOgInnkreving(
                    opprettSøknad.behandlingstema,
                    opprettSøknad.innkreving,
                )!!
                .kode

        opprettSøknad.barnListe.forEach { barn ->
            try {
                val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                søknadslinjeRepository
                    .save(
                        Søknadslinje(
                            søknadsid = søknadBidrag.søknadsid!!,
                            rolleid = rolleid!!,
                            innbetaltBeløp = barn.innbetaltBeløp,
                            søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                            statusdato = LocalDate.now(),
                            gruppeKombinasjonskode = gruppeKombinasjonskode,
                            saksnummer = opprettSøknad.saksnummer,
                        ),
                    )
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }
        }

        lagHendelseForOpprettetSøknad(søknadBidrag, gruppeKombinasjonskode, opprettSøknad)

        if (opprettSøknad.innkreving) {
            // Det skal i tillegg opprettes en innkrevingssøknad/søknadslinjer
            val søknadInnkreving =
                Søknad(
                    blankettid = blankettid,
                    søknadMottattDato = opprettSøknad.søknadMottattDato,
                    søknadFomDato = opprettSøknad.søknadFomDato,
                    søknadsgruppekode = "IK",
                    behandlerenhet = opprettSøknad.behandlerenhet,
                    saksnummer = opprettSøknad.saksnummer,
                    behandlingsid = opprettSøknad.behandlingsid.toString(),
                )
            søknadRepository.save(søknadInnkreving)

            opprettSøknad.barnListe.forEach { barn ->
                try {
                    val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                    søknadslinjeRepository
                        .save(
                            Søknadslinje(
                                søknadsid = søknadInnkreving.søknadsid!!,
                                rolleid = rolleid!!,
                                innbetaltBeløp = barn.innbetaltBeløp,
                                søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                                statusdato = LocalDate.now(),
                                gruppeKombinasjonskode = SøknadGruppeKombinasjon.BIDRAG_INNKREVING.kode,
                                saksnummer = opprettSøknad.saksnummer,
                            ),
                        )
                } catch (e: Exception) {
                    val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                    secureLogger.error(e) { melding }
                    throw IllegalStateException(melding, e)
                }
            }
        }
        return søknadBidrag.søknadsid!!
    }

    private fun behandleBidrag18År(
        opprettSøknad: OpprettSøknadRequest,
        blankettid: Long,
    ): Long {
        val søknadBidrag =
            opprettOgLagreSøknad(
                opprettSøknad = opprettSøknad,
                blankettid = blankettid,
            )
        val gruppeKombinasjonskode =
            SøknadGruppeKombinasjon
                .fraBehandlingstemaOgInnkreving(
                    opprettSøknad.behandlingstema,
                    opprettSøknad.innkreving,
                )!!
                .kode

        opprettSøknad.barnListe.forEach { barn ->
            try {
                val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                søknadslinjeRepository
                    .save(
                        Søknadslinje(
                            søknadsid = søknadBidrag.søknadsid!!,
                            rolleid = rolleid!!,
                            innbetaltBeløp = barn.innbetaltBeløp,
                            søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                            statusdato = LocalDate.now(),
                            gruppeKombinasjonskode = gruppeKombinasjonskode,
                            saksnummer = opprettSøknad.saksnummer,
                        ),
                    )
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }
        }

        lagHendelseForOpprettetSøknad(søknadBidrag, gruppeKombinasjonskode, opprettSøknad)

        if (opprettSøknad.innkreving) {
            // Det skal i tillegg opprettes en innkrevingssøknad/søknadslinjer
            val søknadInnkreving =
                Søknad(
                    blankettid = blankettid,
                    søknadMottattDato = opprettSøknad.søknadMottattDato,
                    søknadFomDato = opprettSøknad.søknadFomDato,
                    søknadsgruppekode = "IK",
                    behandlerenhet = opprettSøknad.behandlerenhet,
                    saksnummer = opprettSøknad.saksnummer,
                    behandlingsid = opprettSøknad.behandlingsid.toString(),
                )
            søknadRepository.save(søknadInnkreving)

            opprettSøknad.barnListe.forEach { barn ->
                try {
                    val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                    søknadslinjeRepository
                        .save(
                            Søknadslinje(
                                søknadsid = søknadInnkreving.søknadsid!!,
                                rolleid = rolleid!!,
                                innbetaltBeløp = barn.innbetaltBeløp,
                                søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                                statusdato = LocalDate.now(),
                                gruppeKombinasjonskode = SøknadGruppeKombinasjon.BIDRAG_18_ÅR_INNKREVING.kode,
                                saksnummer = opprettSøknad.saksnummer,
                            ),
                        )
                } catch (e: Exception) {
                    val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                    secureLogger.error(e) { melding }
                    throw IllegalStateException(melding, e)
                }
            }
        }
        return søknadBidrag.søknadsid!!
    }

    private fun behandleRevurderingForskudd(
        opprettSøknad: OpprettSøknadRequest,
        blankettid: Long,
    ): Long {
        val søknadForskudd =
            opprettOgLagreSøknad(
                opprettSøknad = opprettSøknad,
                blankettid = blankettid,
            )

        opprettSøknad.barnListe.forEach { barn ->
            try {
                val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                søknadslinjeRepository
                    .save(
                        Søknadslinje(
                            søknadsid = søknadForskudd.søknadsid!!,
                            rolleid = rolleid!!,
                            innbetaltBeløp = barn.innbetaltBeløp,
                            søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                            statusdato = LocalDate.now(),
                            gruppeKombinasjonskode = Behandlingstema.FORSKUDD.bisysKode,
                            saksnummer = opprettSøknad.saksnummer,
                        ),
                    )
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }
        }

        // TODO: Dette fungerer ikke for forskudd. SKal ikke opprette FF hendelse
        lagHendelseForOpprettetSøknad(søknadForskudd, Behandlingstema.FORSKUDD.bisysKode, opprettSøknad)
        return søknadForskudd.søknadsid!!
    }

    private fun behandleOppfostringsbidrag(
        opprettSøknad: OpprettSøknadRequest,
        blankettid: Long,
    ): Long {
        val søknadBidrag =
            opprettOgLagreSøknad(
                opprettSøknad = opprettSøknad,
                blankettid = blankettid,
            )

        opprettSøknad.barnListe.forEach { barn ->
            try {
                val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                søknadslinjeRepository
                    .save(
                        Søknadslinje(
                            søknadsid = søknadBidrag.søknadsid!!,
                            rolleid = rolleid!!,
                            innbetaltBeløp = barn.innbetaltBeløp,
                            søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                            statusdato = LocalDate.now(),
                            gruppeKombinasjonskode = SøknadGruppeKombinasjon.OPPFOSTRINGSBIDRAG_INNKREVING.kode,
                            saksnummer = opprettSøknad.saksnummer,
                        ),
                    )
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }
        }

        lagHendelseForOpprettetSøknad(
            søknadBidrag,
            SøknadGruppeKombinasjon.OPPFOSTRINGSBIDRAG_INNKREVING.kode,
            opprettSøknad,
        )

        if (opprettSøknad.innkreving) {
            // Det skal i tillegg opprettes en innkrevingssøknad/søknadslinjer
            val søknadInnkreving =
                Søknad(
                    blankettid = blankettid,
                    søknadMottattDato = opprettSøknad.søknadMottattDato,
                    søknadFomDato = opprettSøknad.søknadFomDato,
                    søknadsgruppekode = "IK",
                    behandlerenhet = opprettSøknad.behandlerenhet,
                    saksnummer = opprettSøknad.saksnummer,
                    behandlingsid = opprettSøknad.behandlingsid.toString(),
                )
            søknadRepository.save(søknadInnkreving)

            opprettSøknad.barnListe.forEach { barn ->
                try {
                    val rolleid = rolleRepository.finnBaRmISak(opprettSøknad.saksnummer, barn.personident).first().rolleid
                    søknadslinjeRepository
                        .save(
                            Søknadslinje(
                                søknadsid = søknadBidrag.søknadsid!!,
                                rolleid = rolleid!!,
                                innbetaltBeløp = barn.innbetaltBeløp,
                                søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                                statusdato = LocalDate.now(),
                                gruppeKombinasjonskode = SøknadGruppeKombinasjon.OPPFOSTRINGSBIDRAG_INNKREVING.kode,
                                saksnummer = opprettSøknad.saksnummer,
                            ),
                        )
                } catch (e: Exception) {
                    val melding = "Feil ved opprettelse av søknadslinje for barn med personident: ${barn.personident}"
                    secureLogger.error(e) { melding }
                    throw IllegalStateException(melding, e)
                }
            }
        }
        return søknadBidrag.søknadsid!!
    }

    private fun opprettOgLagreSøknad(
        opprettSøknad: OpprettSøknadRequest,
        blankettid: Long,
    ): Søknad {
        val søknad =
            try {
                val nySøknad =
                    Søknad(
                        blankettid = blankettid,
                        søknadMottattDato = opprettSøknad.søknadMottattDato,
                        refVedtaksid = opprettSøknad.refVedtaksid,
                        søknadFomDato = opprettSøknad.søknadFomDato,
                        søknadsgruppekode = opprettSøknad.behandlingstema.bisysKode,
                        behandlerenhet = opprettSøknad.behandlerenhet,
                        saksnummer = opprettSøknad.saksnummer,
                        referertSøknadsid = opprettSøknad.refSøknadsid,
                        behandlingsid = opprettSøknad.behandlingsid.toString(),
                    )
                søknadRepository.save(nySøknad)
            } catch (e: Exception) {
                val melding = "Feil ved opprettelse av søknad for saksnummer: ${opprettSøknad.saksnummer}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }

        return søknad
    }

    private fun lagHendelseForOpprettetSøknad(
        søknad: Søknad,
        gruppeKombinasjonskode: String,
        opprettSøknad: OpprettSøknadRequest,
    ) {
        val opprettetAv =
            TokenUtils.hentSaksbehandlerIdent()
                ?: TokenUtils.hentApplikasjonsnavn()

        try {
            // TODO: Dette fungerer ikke for revurder forskudd!
            secureLogger.info { "Lagrer hendelse for opprettet søknad med id: ${søknad.søknadsid}" }
            hendelseRepository.save(
                Hendelse(
                    saksnummer = søknad.saksnummer,
                    hendelsestype = bestemHendelseType(opprettSøknad),
                    opprettetDato = LocalDateTime.now(),
                    enhet = søknad.behandlerenhet!!,
                    søknadstype = opprettSøknad.behandlingstype?.bisysKode ?: Behandlingstype.FORHOLDSMESSIG_FORDELING.bisysKode,
                    opprettetAv = opprettetAv,
                    gruppeKombinasjonskode = gruppeKombinasjonskode,
                    blankettid = søknad.blankettid,
                    søknadsid = søknad.søknadsid!!,
                    systemOpprettetDato = LocalDateTime.now(),
                ),
            )
        } catch (e: Exception) {
            val melding = "Feil ved opprettelse av hendelse for søknad med id: ${søknad.søknadsid}"
            secureLogger.error(e) { melding }
            throw IllegalStateException(melding, e)
        }
    }

    private fun bestemHendelseType(opprettSøknad: OpprettSøknadRequest): String {
        val søktAv = opprettSøknad.søktAv ?: SøktAvType.NAV_BIDRAG
        val behandlingstype = opprettSøknad.behandlingstype ?: Behandlingstype.FORHOLDSMESSIG_FORDELING
        return "${behandlingstype.bisysKode}${søktAv.kode}"
    }

    fun oppdaterBehandlingsid(request: OppdaterBehandlingsidRequest) {
        secureLogger.info { "Request mottatt for å oppdatere behandlingsid: $request " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                request.søknadFinnesIkke()
            }
        if (request.eksisterendeBehandlingsid != søknad.behandlingsid?.toLong()) {
            val melding =
                "Det er mismatch mellom angitt eksisterende behandlingsid og lagret behandlingsid for søknad med id: " +
                    "${request.søknadsid}. Angitt eksisterende behandlingsid: ${request.eksisterendeBehandlingsid}. " +
                    "Lagret behandlingsid: ${søknad.behandlingsid}"
            secureLogger.error { melding }
            request.mismatchEksisterendeBehandlingsid()
        }

        // Oppdaterer alle søknader under blankett
        val søknader = søknadRepository.finnAlleTilhørendeSøknader(søknad.blankettid)
        søknader.forEach {
            it.behandlingsid = request.nyBehandlingsid.toString()
            søknadRepository.save(it)
        }
    }

    fun oppdaterBehandlerenhet(request: OppdaterBehandlerenhetRequest) {
        secureLogger.info { "Request mottatt for å oppdatere behandlerenhet på søknad: $request " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        // Oppdater alle søknader under blankett
        val søknader = søknadRepository.finnAlleTilhørendeSøknader(søknad.blankettid)
        søknader.forEach {
            it.behandlerenhet = request.behandlerenhet
            søknadRepository.save(it)
        }
    }

    fun feilregistrerSøknad(request: FeilregistrerSøknadRequest) {
        secureLogger.info { "Request mottatt for å feilregistrere søknad: ${request.søknadsid} " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        val søknadslinjer = søknadslinjeRepository.finnSøknadslinjerForSøknad(request.søknadsid)

        if (søknadslinjer.isEmpty()) {
            val melding = "Fant ingen søknadslinjer for søknad med id: ${request.søknadsid}"
            secureLogger.error { melding }
            return
        }

        if (søknadslinjer.all { it.søknadStatuskode == Behandlingstatus.FEILREGISTRERT.bisysKode }) {
            val melding = "Søknad med id: ${request.søknadsid} er allerede feilregistrert"
            secureLogger.error { melding }
            return
        }

        søknadslinjer.forEach {
            it.søknadStatuskode = Behandlingstatus.FEILREGISTRERT.bisysKode
            it.statusdato = LocalDate.now()
            søknadslinjeRepository.save(it)
        }

        val gruppeKombinasjonskode = søknadslinjer.first().gruppeKombinasjonskode
        val søknadstype = `finnSøknadstype`(søknad)

        // Lag hendelse for feilreistrering av søknad
        lagHendelseForFeilregistreringAvSøknad(søknad, gruppeKombinasjonskode, søknadstype)

        // Feilregistrer relatert innkrevingssøknad
        val tilhørendeInnkrevingssøknad =
            søknadRepository.finnTilhørendeInnkrevingsssøknad(søknad.blankettid)

        if (tilhørendeInnkrevingssøknad == null) {
            secureLogger.info { "Fant ingen tilhørende innkrevingssøknad å feilregistrere for søknad med id: ${søknad.søknadsid}" }
            return
        }
        val søknadslinjerInnkrevingssøknad = søknadslinjeRepository.finnSøknadslinjerForSøknad(tilhørendeInnkrevingssøknad.søknadsid!!)
        søknadslinjerInnkrevingssøknad.forEach {
            it.søknadStatuskode = Behandlingstatus.FEILREGISTRERT.bisysKode
            it.statusdato = LocalDate.now()
            søknadslinjeRepository.save(it)
        }

        // Slett eventuell søknadsknytning
        slettSammenknytningReferertSøknad(SlettSammenknytningForSøknadRequest(søknadsid = request.søknadsid))
    }

    fun feilregistrerSøknadsbarn(request: FeilregistrerSøknadsBarnRequest) {
        secureLogger.info { "Request mottatt for å feilregistrere søknadsbarn: ${request.personidentBarn} i søknad: ${request.søknadsid} " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        val rolleid =
            rolleRepository
                .finnRollerISak(søknad.saksnummer)
                .firstOrNull { it.fnr == request.personidentBarn }
                ?.rolleid ?: throw HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Fant ikke rolle for barn med personident: ${request.personidentBarn}",
            )

        val søknadslinjer = søknadslinjeRepository.finnSøknadslinjerForSøknad(request.søknadsid)
        val søknadslinjeBarn = søknadslinjer.filter { it.rolleid == rolleid }

        søknadslinjeBarn.forEach {
            it.søknadStatuskode = Behandlingstatus.FEILREGISTRERT.bisysKode
            it.statusdato = LocalDate.now()
            søknadslinjeRepository.save(it)
        }

        val gruppeKombinasjonskode = søknadslinjer.first().gruppeKombinasjonskode
        val søknadstype = `finnSøknadstype`(søknad)

        // Sjekk om alle søknadslinjer er feilregistrert og lag hendelse for søknaden hvis det er tilfelle
        if (søknadslinjer.all { it.søknadStatuskode == Behandlingstatus.FEILREGISTRERT.bisysKode }) {
            lagHendelseForFeilregistreringAvSøknad(søknad, gruppeKombinasjonskode, søknadstype)
        }

        // Feilregistrer relatert innkrevingssøknad
        val tilhørendeInnkrevingssøknad =
            søknadRepository.finnTilhørendeInnkrevingsssøknad(søknad.blankettid)

        if (tilhørendeInnkrevingssøknad == null) {
            secureLogger.info { "Fant ingen tilhørende innkrevingssøknad å feilregistrere for søknad med id: ${søknad.søknadsid}" }
            return
        }
        val søknadslinjerInnkrevingssøknad = søknadslinjeRepository.finnSøknadslinjerForSøknad(tilhørendeInnkrevingssøknad.søknadsid!!)
        søknadslinjerInnkrevingssøknad
            .filter { it.rolleid == rolleid }
            .forEach {
                it.søknadStatuskode = Behandlingstatus.FEILREGISTRERT.bisysKode
                it.statusdato = LocalDate.now()
                søknadslinjeRepository.save(it)
            }
    }

    private fun finnSøknadstype(søknad: Søknad): Behandlingstype = try {
        blankettRepository.finnSøknadstype(søknad.blankettid).let { Behandlingstype.fraKode(it) } ?: Behandlingstype.SØKNAD
    } catch (e: Exception) {
        Behandlingstype.SØKNAD
    }

    fun leggTilBarnIFFSøknad(request: LeggTilBarnIFFSøknadRequest) {
        secureLogger.info { "Request mottatt for å legge til barn ${request.personidentBarn} i FF-søknad: ${request.søknadsid} " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        val blankett =
            blankettRepository.findById(søknad.blankettid.toString()).orElseThrow {
                val melding = "Fant ikke blankett med id: ${søknad.blankettid}"
                secureLogger.error { melding }
                IllegalStateException(melding)
            }

        if (!søknadstyperFFKoder.contains(blankett.søknadstype)) {
            val melding = "Søknad med id: ${request.søknadsid} er ikke en FF-søknad"
            secureLogger.error { melding }
            throw HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
        }

        val rolleid =
            rolleRepository
                .finnRollerISak(søknad.saksnummer)
                .firstOrNull { it.fnr == request.personidentBarn }
                ?.rolleid ?: throw HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Fant ikke rolle for barn med personident: ${request.personidentBarn}",
            )

        val søknadslinjer =
            søknadslinjeRepository.finnSøknadslinjerForSøknad(søknad.søknadsid!!)

        val søknadslinjeBarn =
            søknadslinjeRepository.finnSøknadslinjerForSøknad(søknad.søknadsid!!).firstOrNull { it.rolleid == rolleid }

        if (søknadslinjeBarn != null) {
            // Barnet er allerede lagt til i søknaden, logg melding, ikke kast exception.
            // Om det er feilregistrert så skal søknaden resettes til 'under behandling'.
            if (søknadslinjeBarn.søknadStatuskode != Behandlingstatus.FEILREGISTRERT.bisysKode) {
                val melding = "Barn med personident: ${request.personidentBarn} er allerede lagt til i søknad med id: ${request.søknadsid}"
                secureLogger.info { melding }
            } else {
                // Barnet er tidligere feilregistrert, oppdaterer status tilbake til UB
                søknadslinjeBarn.søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode
                søknadslinjeBarn.statusdato = LocalDate.now()
                søknadslinjeRepository.save(søknadslinjeBarn)
                secureLogger.info {
                    "Barn med personident: ${request.personidentBarn} er lagt til på nytt i søknad med " +
                        "id: ${request.søknadsid}. Status er resatt til under behandling"
                }
                return
            }
        }

        val gruppeKombinasjonskode = søknadslinjer.first().gruppeKombinasjonskode

        try {
            søknadslinjeRepository
                .save(
                    Søknadslinje(
                        søknadsid = søknad.søknadsid!!,
                        rolleid = rolleid,
                        innbetaltBeløp = request.innbetaltBeløp,
                        søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                        statusdato = LocalDate.now(),
                        gruppeKombinasjonskode = gruppeKombinasjonskode,
                        saksnummer = søknad.saksnummer,
                    ),
                )
        } catch (e: Exception) {
            val melding = "Feil ved opprettelse av søknadslinje for nytt barn lagt til i søknad. Personident: ${request.personidentBarn}"
            secureLogger.error(e) { melding }
            throw IllegalStateException(melding, e)
        }

        val tilhørendeInnkrevingssøknad =
            søknadRepository.finnTilhørendeInnkrevingsssøknad(søknad.blankettid)

        if (tilhørendeInnkrevingssøknad != null) {
            // Det skal opprettes søknadslinje tilknyttet innkrevingsøknaden for barnet som er lagt til søknaden
            try {
                søknadslinjeRepository
                    .save(
                        Søknadslinje(
                            søknadsid = tilhørendeInnkrevingssøknad.søknadsid!!,
                            rolleid = rolleid,
                            innbetaltBeløp = null,
                            søknadStatuskode = Behandlingstatus.UNDER_BEHANDLING.bisysKode,
                            statusdato = LocalDate.now(),
                            gruppeKombinasjonskode = SøknadGruppeKombinasjon.BIDRAG_INNKREVING.kode,
                            saksnummer = søknad.saksnummer,
                        ),
                    )
            } catch (e: Exception) {
                val melding =
                    "Feil ved opprettelse av søknadslinje på innkrevingssøknad for barn med " +
                        "personident: ${request.personidentBarn}"
                secureLogger.error(e) { melding }
                throw IllegalStateException(melding, e)
            }
        }
    }

    fun hentSøknad(request: HentSøknadRequest): HentSøknadResponse {
        secureLogger.info { "Request mottatt for å hente søknad med id: ${request.søknadsid} " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        val søknadslinjer = søknadslinjeRepository.finnSøknadslinjerForSøknad(request.søknadsid)

        val vedtaksoverføring = vedtakOverføringRepository.finnForSøknadsid(request.søknadsid)

        val partISøknadListe =
            finnPartISøknad(
                søknad.blankettid,
                søknad.saksnummer,
                søknadslinjer,
            )
        val søknadstype = finnSøknadstype(søknad)
        val søknadHarInnkreving =
            søknadRepository
                .finnTilhørendeInnkrevingssøknaderOgSøknadslinjer(
                    søknad.blankettid!!,
                ).isNotEmpty()
        return HentSøknadResponse(
            HentSøknad(
                søknadsid = søknad.søknadsid!!,
                søknadMottattDato = søknad.søknadMottattDato,
                søknadFomDato = søknad.søknadFomDato,
                behandlingstema = Behandlingstema.fraKode(søknad.søknadsgruppekode)!!,
                behandlerenhet = søknad.behandlerenhet,
                behandlingstype = søknadstype,
                refVedtaksid = søknad.refVedtaksid,
                refSøknadsid = søknad.referertSøknadsid,
                saksnummer = søknad.saksnummer,
                vedtaksid = vedtaksoverføring?.vedtakIdBidragVedtak,
                behandlingsid = søknad.behandlingsid?.toLong(),
                behandlingStatusType = finnBehandlingsstatusType(søknadslinjer),
                partISøknadListe = partISøknadListe,
                innkreving = søknadHarInnkreving,
                søktAvType = SøktAvType.fraKode(blankettRepository.finnSøknadFraKode(søknad.blankettid))!!,
            ),
        )
    }

    private fun erFFSøknad(søknadsid: Long): Boolean {
        val søknad =
            søknadRepository.findById(søknadsid.toString()).getOrNull() ?: return false
        return finnSøknadstype(søknad).erForholdsmessigFordeling()
    }

    fun oppdaterReferanseGebyr(request: OppdaterReferanseGebyrRequest) {
        secureLogger.info { "Request mottatt for å oppdatere referanse på gebyrsøknad tilknyttet søknad: $request " }
        val søknad =
            søknadRepository.findById(request.søknadsid.toString()).orElseThrow {
                val melding = "Fant ikke søknad med id: ${request.søknadsid}"
                secureLogger.error { melding }
                HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
            }

        val søknadslinjer =
            søknadslinjeRepository.finnSøknadslinjerForSøknad(request.søknadsid)
        val alleBehandlingsstatuser = søknadslinjer.map { Behandlingstatus.fraKode(it.søknadStatuskode) }

        if (alleBehandlingsstatuser.all { it?.lukketStatus == true }) {
            val melding =
                "Referanse på gebyr kan ikke oppdateres hvis søknaden er lukket. " +
                    "søknadsid: ${request.søknadsid} personident: ${request.personident}"
            throw HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
        }

        val rolleid =
            rolleRepository
                .finnRollerISak(søknad.saksnummer)
                .firstOrNull { it.fnr == request.personident }
                ?.rolleid ?: throw IllegalArgumentException("Fant ikke rolle for personident: ${request.personident}")

        // Finn gebyrsøknad for så å finn søknadslinje tilknyttet angitt personident og oppdater denne med referanse
        val gebyrsøknad =
            søknadRepository.finnAlleTilhørendeSøknader(søknad.blankettid).find { it.søknadsgruppekode == "GB" }
                ?: throw HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Fant ikke gebyrsøknad tilknyttet søknad med id: ${request.søknadsid}",
                )

        val søknadslinje =
            søknadslinjeRepository.finnSøknadslinjerForSøknad(gebyrsøknad.søknadsid!!).find { it.rolleid == rolleid }
                ?: throw HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Fant ikke søknadslinje for personident: ${request.personident} i gebyrsøknad med id: ${gebyrsøknad.søknadsid}",
                )
        søknadslinje.engangsbeløpReferanse = request.referanse
        søknadslinjeRepository.save(søknadslinje)
    }

    private fun finnBehandlingsstatusType(søknadslinjeListe: List<Søknadslinje>): BehandlingStatusType {
        val alleBehandlingsstatuser = søknadslinjeListe.map { Behandlingstatus.fraKode(it.søknadStatuskode) }

        val alleLukket = alleBehandlingsstatuser.all { it?.lukketStatus == true }
        val harVedtakFattet = alleBehandlingsstatuser.any { it?.bisysKode == "VF" }
        val harFeilregistretSøknad = alleBehandlingsstatuser.any { it?.bisysKode == "FR" }

        return when {
            alleLukket && harVedtakFattet -> BehandlingStatusType.VEDTAK_FATTET
            alleLukket && harFeilregistretSøknad -> BehandlingStatusType.AVBRUTT
            else -> BehandlingStatusType.UNDER_BEHANDLING
        }
    }

    private fun lagHendelseForFeilregistreringAvSøknad(
        søknad: Søknad,
        gruppeKombinasjonskode: String,
        søknadstype: Behandlingstype,
    ) {
        val opprettetAv =
            TokenUtils.hentSaksbehandlerIdent()
                ?: TokenUtils.hentApplikasjonsnavn()
        try {
            secureLogger.info { "Lager hendelse for feilregistrering av søknad med id: ${søknad.søknadsid}" }
            hendelseRepository.save(
                Hendelse(
                    saksnummer = søknad.saksnummer,
                    hendelsestype = "FR",
                    opprettetDato = LocalDateTime.now(),
                    enhet = søknad.behandlerenhet!!,
                    søknadstype = søknadstype.bisysKode,
                    opprettetAv = opprettetAv,
                    gruppeKombinasjonskode = gruppeKombinasjonskode,
                    blankettid = søknad.blankettid,
                    søknadsid = søknad.søknadsid!!,
                    systemOpprettetDato = LocalDateTime.now(),
                ),
            )
        } catch (e: Exception) {
            val melding = "Feil ved opprettelse av hendelse for søknad med id: ${søknad.søknadsid}"
            secureLogger.error(e) { melding }
            throw IllegalStateException(melding, e)
        }
    }

    fun sammenknyttSøknader(request: SammenknyttSøknaderRequest) {
        if (søknadRepository.finnSøknad(request.hovedsøknadsid) == null || søknadRepository.finnSøknad(request.referertSøknadsid) == null) {
            val melding =
                "Fant ikke søknad med id: ${request.hovedsøknadsid} eller ${request.referertSøknadsid} for sammenknytning"
            secureLogger.error { melding }
            throw HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
        }

        // Sjekk om sammenknytning finnes fra før
        if (søknadsknytningRepository.finnSøknadsknytning(
                hovedsøknadsid = request.hovedsøknadsid,
                referertSøknadsid = request.referertSøknadsid,
                status = SøknadsknytningStatus.Aktiv.name,
            ) != null
        ) {
            val melding =
                "Søknader med id: ${request.hovedsøknadsid} og ${request.referertSøknadsid} er allerede sammenknyttet"
            secureLogger.warn { melding }
            return
        }
        try {
            val nySammenknytning =
                Søknadsknytning(
                    hovedsøknadsid = request.hovedsøknadsid,
                    referertSøknadsid = request.referertSøknadsid,
                    status = SøknadsknytningStatus.Aktiv.name,
                    søknadKnytningstype = "FF",
                    opprettetTidspunkt = LocalDateTime.now(),
                )
            søknadsknytningRepository.save(nySammenknytning)
        } catch (e: Exception) {
            val melding = "Feil ved sammenknytning av søknader med id: ${request.hovedsøknadsid} ${request.referertSøknadsid}"
            secureLogger.error(e) { melding }
            throw IllegalStateException(melding, e)
        }
    }

    fun slettSammenknytningReferertSøknad(request: SlettSammenknytningForSøknadRequest) {
        val søknadsknytninger =
            søknadsknytningRepository
                .finnSøknadsknytningReferertSøknad(referertSøknadsid = request.søknadsid, status = SøknadsknytningStatus.Aktiv.name)

        søknadsknytninger
            .forEach {
                it.status = SøknadsknytningStatus.Slettet.name
                søknadsknytningRepository.save(it)
            }

        if (søknadsknytninger.isEmpty()) {
            secureLogger.warn { "Ingen sammenknytninger funnet for søknad med med id: ${request.søknadsid} " }
            return
        }
    }

    @Transactional("bisysTransactionManager")
    fun endreSammenknytningSøknad(request: SammenknyttSøknaderRequest): Søknadsknytning? {
        // Først finn eksisterende sammenknytning og deaktiver denne
        val eksisterendeSammenknytninger =
            søknadsknytningRepository
                .finnSøknadsknytningReferertSøknad(
                    referertSøknadsid = request.referertSøknadsid,
                    status = SøknadsknytningStatus.Aktiv.name,
                )

        if (eksisterendeSammenknytninger.isEmpty()) {
            secureLogger.warn { "Ingen sammenknytninger funnet for søknad med med id: ${request.referertSøknadsid} " }
            return null
        }

        // Sjekk om sammenknytning mot hovedsøknad finnes fra før
        val eksisterendeSammenknytningNyHovedsøknad =
            eksisterendeSammenknytninger.firstOrNull { it.hovedsøknadsid == request.hovedsøknadsid }
        if (eksisterendeSammenknytningNyHovedsøknad != null) {
            val melding =
                "Søknader med id: ${request.hovedsøknadsid} og ${request.referertSøknadsid} er allerede sammenknyttet"
            secureLogger.warn { melding }
            return eksisterendeSammenknytningNyHovedsøknad
        }

        // Sletter eksisterende knytninger
        eksisterendeSammenknytninger.forEach {
            it.status = SøknadsknytningStatus.Slettet.name
        }
        søknadsknytningRepository.saveAll(eksisterendeSammenknytninger)

        val nySammenknytning =
            Søknadsknytning(
                hovedsøknadsid = request.hovedsøknadsid,
                referertSøknadsid = request.referertSøknadsid,
                status = SøknadsknytningStatus.Aktiv.name,
                søknadKnytningstype = "FF",
                opprettetTidspunkt = LocalDateTime.now(),
            )
        søknadsknytningRepository.save(nySammenknytning)
        return nySammenknytning
    }

    fun slettSammenknytningerHovedsøknad(request: SlettHovedsøknadRequest) {
        // Finn eksisterende sammenknytninger og deaktiver disse
        val eksisterendeSammenknytninger =
            søknadsknytningRepository
                .finnSøknadsknytningerHovedsøknad(
                    hovedsøknadsid = request.eksisterendeHovedsøknadsid,
                    status = SøknadsknytningStatus.Aktiv.name,
                ).ifEmpty {
                    finnSammenknytningHovedsøknadForReferertSøknad(
                        FinnSammenknytningerHovedsøknadRequest(request.eksisterendeHovedsøknadsid, status = SøknadsknytningStatus.Aktiv),
                    )
                }

        if (eksisterendeSammenknytninger.isEmpty()) {
            secureLogger.warn { "Ingen sammenknytninger funnet for søknad med med id: ${request.eksisterendeHovedsøknadsid} " }
            return
        }

        eksisterendeSammenknytninger
            .forEach {
                it.status = SøknadsknytningStatus.Slettet.name
                søknadsknytningRepository.save(it)
            }

        if (request.nyHovedsøknadsid != null) {
            val nyeKnytninger =
                eksisterendeSammenknytninger
                    .filter { it.referertSøknadsid != request.eksisterendeHovedsøknadsid }
                    .map {
                        Søknadsknytning(
                            hovedsøknadsid = request.nyHovedsøknadsid,
                            referertSøknadsid = it.referertSøknadsid,
                            status = SøknadsknytningStatus.Aktiv.name,
                            søknadKnytningstype = it.søknadKnytningstype,
                            opprettetTidspunkt = LocalDateTime.now(),
                        )
                    }
            søknadsknytningRepository.saveAll(nyeKnytninger)
        } else if (request.feilregistrerFFSøknader) {
            eksisterendeSammenknytninger
                .filter { it.referertSøknadsid != null && erFFSøknad(it.referertSøknadsid!!) }
                .forEach {
                    feilregistrerSøknad(FeilregistrerSøknadRequest(it.referertSøknadsid!!))
                }
        }
    }

    fun finnSammenknytningerHovedsøknad(request: FinnSammenknytningerHovedsøknadRequest): FinnSammenknytningerHovedsøknadResponse {
        // Finn eksisterende sammenknytninger for angitt hovedsøknad
        val sammenknytninger =
            søknadsknytningRepository
                .finnSøknadsknytningerHovedsøknad(hovedsøknadsid = request.søknadsid, status = request.status.name)
                .ifEmpty {
                    finnSammenknytningHovedsøknadForReferertSøknad(request)
                }

        val hoveddsøknad = sammenknytninger.firstOrNull()?.hovedsøknadsid ?: request.søknadsid
        if (sammenknytninger.isEmpty()) {
            secureLogger.warn { "Ingen sammenknytninger funnet for søknad med med id: ${request.søknadsid} " }
            return FinnSammenknytningerHovedsøknadResponse(hoveddsøknad, emptyList())
        }

        val søknader =
            sammenknytninger
                .map {
                    hentSøknad(HentSøknadRequest(it.referertSøknadsid!!)).søknad
                }

        return FinnSammenknytningerHovedsøknadResponse(hoveddsøknad, søknader)
    }

    private fun finnSammenknytningHovedsøknadForReferertSøknad(request: FinnSammenknytningerHovedsøknadRequest): List<Søknadsknytning> {
        val sammenknytning =
            søknadsknytningRepository
                .finnSøknadsknytningReferertSøknad(
                    referertSøknadsid = request.søknadsid,
                    status = request.status.name,
                ).firstOrNull() ?: return emptyList()

        val hovedsøknadsid = sammenknytning.hovedsøknadsid ?: return emptyList()

        return søknadsknytningRepository
            .finnSøknadsknytningerHovedsøknad(hovedsøknadsid = hovedsøknadsid, status = request.status.name)
    }
}
