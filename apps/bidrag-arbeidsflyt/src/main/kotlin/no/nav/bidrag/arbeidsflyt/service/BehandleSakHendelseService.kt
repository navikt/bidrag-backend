package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.persistence.entity.Sak
import no.nav.bidrag.arbeidsflyt.persistence.repository.SakRepository
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleSakHendelseService(
    private val sakConsumer: BidragSakConsumer,
    private val sakRepository: SakRepository,
    private val oppgaveService: OppgaveService,
) {
    @Transactional
    fun behandleHendelse(hendelse: SakHendelse) {
        if (hendelse.hendelsestype != SakKafkaHendelsestype.ENDRING) return

        val saksnummer = hendelse.saksnummer.verdi
        val sakDto = sakConsumer.hentSakUtenCache(saksnummer)
        val eksisterende = sakRepository.findBySaksnummer(saksnummer)
        val forrigeKategori = eksisterende?.kategori

        val sak =
            eksisterende?.apply {
                sak = sakDto
                kategori = sakDto.kategori
                eierfogd = sakDto.eierfogd.verdi
                saksstatus = sakDto.saksstatus
                arbeidsfordeling = sakDto.arbeidsfordeling
                opprettetDato = sakDto.opprettetDato
                avsluttet = sakDto.avsluttet
                endretTidspunkt = LocalDateTime.now()
            } ?: Sak(
                saksnummer = saksnummer,
                sak = sakDto,
                kategori = sakDto.kategori,
                eierfogd = sakDto.eierfogd.verdi,
                saksstatus = sakDto.saksstatus,
                arbeidsfordeling = sakDto.arbeidsfordeling,
                opprettetDato = sakDto.opprettetDato,
                avsluttet = sakDto.avsluttet,
            )
        sakRepository.save(sak)

        if (forrigeKategori != null && forrigeKategori != sakDto.kategori) {
            LOGGER.info { "Sakskategori endret fra $forrigeKategori til ${sakDto.kategori} for sak ${saksnummer.sanitizeForLog()}" }
            oppgaveService.endreBehandlingstypeForSak(saksnummer, sakDto.kategori)
        }
    }
}
