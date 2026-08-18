package no.nav.bidrag.dokument.journalpost.consumer.mq

import jakarta.transaction.Transactional
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.exception.BehandlingAvBrevkvitteringFeilet
import no.nav.bidrag.dokument.journalpost.hendelse.DokumentKafkaHendelseProdusent
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.mq.BrevKvittering
import no.nav.bidrag.dokument.journalpost.mq.BrevStatus
import no.nav.bidrag.dokument.journalpost.service.JournalpostService
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentHendelse
import no.nav.bidrag.transport.dokument.DokumentHendelseType
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class BrevserverKvitteringListener(
    private val journalpostService: JournalpostService,
    private val journalpostKafkaEventProducer: JournalpostKafkaEventProducer,
    private val dokumentKafkaHendelseProdusent: DokumentKafkaHendelseProdusent,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BrevserverKvitteringListener::class.java)
        private val FORSENDELSE_PREFIKS = "BIF"
    }

    @Transactional
    @JmsListener(destination = $$"${BREVSERVER_KVITTERING_QUEUE}", containerFactory = "brevserverKvitteringListenerFactory")
    @Retryable(maxAttempts = 5, backoff = Backoff(delay = 5000, maxDelay = 60000, multiplier = 2.0))
    fun receiveMessage(brevKvittering: BrevKvittering) {
        LOGGER.info("Behandler brevkvittering med dokref={} og status={}", brevKvittering.brevRef, brevKvittering.status)
        SECURE_LOGGER.info("Behandler brevkvittering {}", brevKvittering)
        validerBrevKvittering(brevKvittering)
        try {
            journalpostService
                .hentJournalpostForDokumentReferanse(brevKvittering.brevRef)
                ?.run { oppdaterJournalpost(brevKvittering, this) }
                ?: behandleJournalpostIkkeFunnet(brevKvittering)
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved behandling av brevkvittering med referanse=${brevKvittering.brevRef}. Prøver på nytt", e)
            throw e
        }
    }

    private fun behandleJournalpostIkkeFunnet(brevKvittering: BrevKvittering) {
        if (brevKvittering.brevRef!!.startsWith(FORSENDELSE_PREFIKS)) {
            publiserDokumentHendelse(brevKvittering)
        } else {
            throw BehandlingAvBrevkvitteringFeilet("Fant ikke journalpost med dokref ${brevKvittering.brevRef}")
        }
    }

    private fun oppdaterJournalpost(
        brevKvittering: BrevKvittering,
        journalpost: Journalpost,
    ) {
        when (brevKvittering.status) {
            // Null betyr at det kommer direkte fra exstream og at det er produsert PDF (batchbrev)
            null, BrevStatus.FERDIG -> {
                behandleBrevstatusFerdig(journalpost)
            }

            BrevStatus.LAGRET -> {
                behandleBrevstatusLagret(journalpost)
            }

            else -> {
                LOGGER.warn(
                    "BrevKvittering (brevref=${brevKvittering.brevRef}, sysid=${brevKvittering.sysId}) " +
                        "har status \"${brevKvittering.status}\", journalpost oppdateres ikke.",
                )
                return
            }
        }
        journalpostService.lagreJournalpost(journalpost)
        publiserJournalpostHendelse(journalpost)
        publiserDokumentHendelse(brevKvittering)
    }

    private fun publiserJournalpostHendelse(journalpost: Journalpost) {
        if (journalpost.erInngaendeDokument()) {
            val hendelse = journalpost.initJournalpostHendelse("9999").copy(enhet = hentJournalforendeEnhet(journalpost))
            journalpostKafkaEventProducer.publish(
                hendelse.copy(
                    sporing =
                    hendelse.sporing?.copy(
                        saksbehandlersNavn = "bidrag-dokument-journalpost",
                    ),
                ),
            )
        }
    }

    private fun publiserDokumentHendelse(brevKvittering: BrevKvittering) {
        dokumentKafkaHendelseProdusent.publish(
            DokumentHendelse(
                dokumentreferanse = brevKvittering.brevRef!!,
                hendelseType =
                when (brevKvittering.status) {
                    // Null betyr at det kommer direkte fra exstream og at det er produsert PDF (batchbrev)
                    null, BrevStatus.FERDIG -> DokumentHendelseType.FERDIGSTILT

                    else -> DokumentHendelseType.ENDRING
                },
                sporingId =
                CorrelationId.fetchCorrelationIdForThread() ?: CorrelationId
                    .generateTimestamped(
                        "brevkvittering_hendelse",
                    ).get(),
                arkivSystem = DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER,
                status =
                when (brevKvittering.status) {
                    null, BrevStatus.FERDIG -> DokumentStatusDto.FERDIGSTILT
                    else -> DokumentStatusDto.UNDER_REDIGERING
                },
            ),
        )
    }

    private fun hentJournalforendeEnhet(journalpost: Journalpost): String? {
        if (journalpost.journalforendeEnhet.isNullOrEmpty()) return null
        val enhetMap = mapOf("2101" to "4865")
        return enhetMap.getOrDefault(journalpost.journalforendeEnhet, journalpost.journalforendeEnhet)
    }

    private fun behandleBrevstatusFerdig(journalpost: Journalpost) {
        if (journalpost.erInngaendeDokument()) {
            oppdaterJournalstatus(journalpost, Journalstatus.MOTTAKSREGISTRERT)
        } else if (journalpost.erUtgaaende()) {
            oppdaterJournalstatus(journalpost, Journalstatus.KLAR_TIL_PRINT)
        } else {
            oppdaterJournalstatus(journalpost, Journalstatus.RESERVERT)
        }
    }

    private fun behandleBrevstatusLagret(journalpost: Journalpost) {
        if (journalpost.erUtgaaende() || journalpost.erNotat()) {
            oppdaterJournalstatus(journalpost, Journalstatus.UNDER_PRODUKSJON)
        }
    }

    private fun oppdaterJournalstatus(
        journalpost: Journalpost,
        newJournalstatus: String,
    ) {
        val prevJournalstatus = journalpost.journalstatus
        journalpost.journalstatus = newJournalstatus
        LOGGER.info(
            "Oppdatert journalpost ${journalpost.journalpostId} journalstatus fra $prevJournalstatus til $newJournalstatus. " +
                "(journalposttype=${journalpost.dokumentType}, dokref=${journalpost.dokumentreferanse})",
        )
    }

    private fun validerBrevKvittering(brevKvittering: BrevKvittering) {
        Validate.isTrue(StringUtils.isNotEmpty(brevKvittering.brevRef), "Mangler brevref")
        Validate.isTrue(StringUtils.isNotEmpty(brevKvittering.sysId), "Mangler sysid")
//        Validate.notNull(brevKvittering.status, "Mangler status")
    }
}
