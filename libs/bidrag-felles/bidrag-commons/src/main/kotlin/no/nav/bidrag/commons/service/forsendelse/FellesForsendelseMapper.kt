package no.nav.bidrag.commons.service.forsendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.service.consumers.FellesPersonConsumer
import no.nav.bidrag.commons.service.consumers.FellesSakConsumer
import no.nav.bidrag.commons.service.consumers.FellesSamhandlerConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.transport.dokument.forsendelse.BehandlingInfoDto
import no.nav.bidrag.transport.dokument.forsendelse.JournalTema
import no.nav.bidrag.transport.dokument.forsendelse.MottakerIdentTypeTo
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.forsendelse.OpprettDokumentForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger {}

internal val dokumentMalerAldersjustering =
    mapOf(
        Stønadstype.BIDRAG to "BI01B05",
    )

data class FellesForsendelseBestilling(
    val gjelder: String,
    val mottaker: String,
    val språkkode: Språk,
    val saksnummer: String,
    val dokumentmal: String,
    val behandlingInfoDto: BehandlingInfoDto?,
    val tema: JournalTema = JournalTema.BID,
    val batchId: String? = null,
    val unikReferanse: String? = null,
    val distribuerAutomatisk: Boolean? = false,
)

data class ForsendelseGjelderMottakerInfo(
    val gjelder: String?,
    val mottakerIdent: String?,
    val mottakerRolle: Rolletype,
    val feilBegrunnelse: String? = null,
)

@Service
class FellesForsendelseMapper(
    private val bidragSakConsumer: FellesSakConsumer,
    private val bidragPersonConsumer: FellesPersonConsumer,
    private val samhandlerConsumer: FellesSamhandlerConsumer,
) {
    companion object {
        fun finnDokumentmalAldersjustering(stønadstype: Stønadstype) = dokumentMalerAldersjustering[stønadstype]

        fun byggBehandlingInfoDtoForAldersjustering(
            vedtakId: String,
            stønadstype: Stønadstype,
            barnIBehandling: List<String>,
        ): BehandlingInfoDto =
            BehandlingInfoDto(
                vedtakId = vedtakId,
                stonadType = stønadstype,
                barnIBehandling = barnIBehandling,
                erFattetBeregnet = true,
                soknadType = "EGET_TILTAK",
                soknadFra = SøktAvType.NAV_BIDRAG,
                vedtakType = Vedtakstype.ALDERSJUSTERING,
            )
    }

    fun tilOpprettForsendelseRequest(
        forsendelseBestilling: FellesForsendelseBestilling,
        distribuerAutomatisk: Boolean = false,
    ): OpprettForsendelseForespørsel {
        val mottakerErSamhandler = forsendelseBestilling.mottaker.let { SamhandlerId(it).gyldig() }
        val saksnummer = forsendelseBestilling.saksnummer
        val enhet = finnEierfogd(forsendelseBestilling.saksnummer)
        val navn = if (mottakerErSamhandler) samhandlerConsumer.hentSamhandler(forsendelseBestilling.mottaker)?.navn else null
        val mottaker =
            MottakerTo(
                ident = forsendelseBestilling.mottaker,
                språk = forsendelseBestilling.språkkode.name,
                navn = navn,
                identType =
                    if (mottakerErSamhandler) {
                        MottakerIdentTypeTo.SAMHANDLER
                    } else {
                        MottakerIdentTypeTo.FNR
                    },
            )

        return OpprettForsendelseForespørsel(
            unikReferanse = "${forsendelseBestilling.unikReferanse}_${mottaker.ident}_$saksnummer",
            gjelderIdent = forsendelseBestilling.gjelder,
            mottaker = mottaker,
            saksnummer = saksnummer,
            enhet = enhet,
            batchId = forsendelseBestilling.batchId,
            tema = forsendelseBestilling.tema,
            behandlingInfo = forsendelseBestilling.behandlingInfoDto,
            distribuerAutomatiskEtterFerdigstilling = distribuerAutomatisk,
            dokumenter =
                listOf(
                    OpprettDokumentForespørsel(
                        dokumentmalId = forsendelseBestilling.dokumentmal,
                        bestillDokument = true,
                        ferdigstill = true,
                    ),
                ),
        )
    }

    fun finnMottakerForKravhaver(
        kravhaver: String,
        saksnummer: String,
    ): ForsendelseGjelderMottakerInfo {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        val rolleBarn = sak.roller.find { it.fødselsnummer?.verdi == kravhaver }!!

        val bm =
            sak.bidragsmottaker ?: return ForsendelseGjelderMottakerInfo(
                null,
                null,
                Rolletype.BIDRAGSMOTTAKER,
                feilBegrunnelse = "Sak ${sak.saksnummer} har ingen bidragsmottaker",
            )

        val mottakerGjelderBM = ForsendelseGjelderMottakerInfo(bm.fødselsnummer?.verdi, bm.fødselsnummer?.verdi, Rolletype.BIDRAGSMOTTAKER)
        if (rolleBarn.reellMottaker != null) {
            return finnRmSomMottaker(rolleBarn) ?: mottakerGjelderBM
        }
        return mottakerGjelderBM
    }

    private fun erPersonDød(personident: Personident): Boolean {
        val person = bidragPersonConsumer.hentPerson(personident)
        return person.dødsdato != null
    }

    private fun finnRmSomMottaker(barn: RolleDto): ForsendelseGjelderMottakerInfo? {
        // *** REGEL ***
        // hvis RM er lik barn så betyr det at barnet bor for seg selv. Sendt forsendelse til barnet hvis barnet er over 18
        // hvis RM er institusjon
        // hvis institusjon er VERGE -> forsendelse
        // hvis IKKE VERGE -> sjekk om institusjon er barnevern institusjon
        // -> hvis barnevern inst -> forsendelse
        // -> hvis IKKE barnevern inst -> forsendelse til BM

        val rm = barn.reellMottaker ?: return null
        if (rm.ident.verdi == barn.fødselsnummer!!.verdi) {
            return if (erOver18År(barn.fødselsnummer!!)) {
                secureLogger.debug {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} " +
                        "og barnet er over 18 år. Setter mottaker av forsendelsen til barnet"
                }
                ForsendelseGjelderMottakerInfo(
                    barn.fødselsnummer!!.verdi,
                    barn.fødselsnummer!!.verdi,
                    Rolletype.BARN,
                )
            } else {
                secureLogger.debug {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} " +
                        "og men er under 18 år. Setter mottaker av forsendelsen til BM"
                }
                null
            }
        }
        // sjekker om denne reelle mottaker er verge -> forsendelse skal til RM
        if (rm.verge) {
            secureLogger.debug { "RM ${rm.ident} til barn ${barn.fødselsnummer} er verge. Setter mottaker av forsendelsen til RM" }
            // Hvis reell mottaker eksisterer og er verge, sendes forsendelsen til vergen
            return ForsendelseGjelderMottakerInfo(barn.fødselsnummer!!.verdi, rm.ident.verdi, Rolletype.REELMOTTAKER)
        }

        if (!SamhandlerId(rm.ident.verdi).gyldig()) {
            log.warn {
                "RM har ikke en gyldig samhandlerId ${rm.ident}. Går videre uten å sjekke om RM er Barnevernsinstitusjon"
            }
            return null
        }

        val samhandler =
            samhandlerConsumer.hentSamhandler(rm.ident.verdi) ?: return ForsendelseGjelderMottakerInfo(
                barn.fødselsnummer!!.verdi,
                rm.ident.verdi,
                Rolletype.REELMOTTAKER,
                "Fant ikke samhandler med id ${rm.ident} i bidrag-samhandler",
            )
        if (samhandler.områdekode == Områdekode.BARNEVERNSINSTITUSJON) {
            secureLogger.debug {
                "RM ${rm.ident} til barnet ${barn.fødselsnummer} er barnevernsinstitusjon." +
                    " Setter mottaker av forsendelsen til RM"
            }
            return ForsendelseGjelderMottakerInfo(
                barn.fødselsnummer!!.verdi,
                rm.ident.verdi,
                Rolletype.REELMOTTAKER,
            )
        }
        secureLogger.debug {
            "RM ${rm.ident} til barnet ${barn.fødselsnummer} er ikke barnevernsinstitusjon eller verge." +
                " Setter mottaker av forsendelsen til BM"
        }
        // hvis RM er samhandler, men ikke barnevern institusjon skal brevet sendes til BM
        return null
    }

    private fun erOver18År(personident: Personident): Boolean {
        val fødselsdato = bidragPersonConsumer.hentFødselsdatoForPerson(personident)
        // TODO: Bør dette sjekkes for en eksakt dato (feks 1.Juli?). Ikke behov i aldersjustering pga barnet alltid er under 18
        return fødselsdato != null && fødselsdato.plusYears(18).isBefore(LocalDate.now())
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }
}
