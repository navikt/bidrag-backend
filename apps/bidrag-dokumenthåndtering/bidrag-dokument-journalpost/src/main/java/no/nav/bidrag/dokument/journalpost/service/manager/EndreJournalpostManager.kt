package no.nav.bidrag.dokument.journalpost.service.manager

import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.Journalsak
import no.nav.bidrag.dokument.journalpost.model.EndreJournalpostHendelseData
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.service.SakService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.transport.dokument.HendelseType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.util.function.Consumer

open class EndreJournalpostManager(
    open val applicationEventPublisher: ApplicationEventPublisher,
    open val journalpostRepository: JournalpostRepository,
    open val sakService: SakService,
    open val tokenInformationService: TokenInformationService,
) {
    private var endreJournalpostCommandIntern: EndreJournalpostCommandIntern? = null
    private var endreJournalpostHendelseData: EndreJournalpostHendelseData? = null
    private var endretJournalpost: Journalpost? = null
    private var journalpost: Journalpost? = null

    fun behandle(journalpost: Journalpost?): EndreJournalpostManager {
        this.journalpost = journalpost
        return this
    }

    fun leggTil(endreJournalpostCommandIntern: EndreJournalpostCommandIntern?) {
        this.endreJournalpostCommandIntern = endreJournalpostCommandIntern
    }

    fun oppdaterBrukerinfoForJournalforing() {
        if (endreJournalpostCommandIntern!!.skalJournalfores) {
            endreJournalpostCommandIntern!!.brukerId = tokenInformationService.hentSaksbehandlersBrukerid()
            endreJournalpostCommandIntern!!.journalfortAv = tokenInformationService.hentSaksbehandlersNavn()
        }
    }

    fun endre(): EndreJournalpostManager {
        endreJournalpostHendelseData = EndreJournalpostHendelseData(journalpost!!.tilJournalpostIntern(), endreJournalpostCommandIntern!!)
        endretJournalpost = journalpost!!.endre(endreJournalpostCommandIntern)
        return this
    }

    fun lagreEndringMedOpprettedeSaksrelasjoner() {
        endreJournalpostHendelseData!!.hentTilknyttSaker().forEach(
            Consumer { saksnummer: String? ->
                if (endretJournalpost!!.tilhorerIkkeSak(saksnummer)) {
                    val savedJournalpost = journalpostRepository.save(endretJournalpost!!)
                    sakService.lagre(Journalsak(savedJournalpost, saksnummer))
                }
            },
        )
    }

    fun publishJournalpostHendelse() {
        val hendelse =
            journalpost!!
                .initJournalpostHendelse(endretJournalpost!!.journalforendeEnhet)
                .copy(
                    hendelseType =
                    if (endreJournalpostCommandIntern?.skalJournalfores == true) {
                        HendelseType.JOURNALFORING
                    } else {
                        HendelseType.ENDRING
                    },
                )
        applicationEventPublisher.publishEvent(hendelse)
    }

    fun mapEndretJournalpostTilInternDto(): JournalpostIntern = endretJournalpost!!.tilJournalpostIntern()

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EndreJournalpostManager::class.java)
    }
}
