package no.nav.bidrag.aktoerregister.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.transport.samhandler.SamhandlerKafkaHendelsestype
import no.nav.bidrag.transport.samhandler.Samhandlerhendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger { }

@Service
class SamhandlerhendelseService(private val aktørRepository: AktørRepository, private val aktørService: AktørService) {

    @Transactional
    fun behandleHendelse(hendelse: Samhandlerhendelse) {
        when (hendelse.hendelsestype) {
            SamhandlerKafkaHendelsestype.OPPRETTET -> {
                opprettNySamhandler(hendelse.samhandlerId)
            }

            SamhandlerKafkaHendelsestype.OPPDATERT -> {
                oppdaterSamhandler(hendelse.samhandlerId)
            }

            SamhandlerKafkaHendelsestype.OPPHØRT -> {
                opphørSamhandler(hendelse.samhandlerId)
            }
        }
    }

    private fun opprettNySamhandler(samhandlerId: String) {
        LOGGER.info { "Skatt melder på ønskede samhandler selv. Ingen handling nødvendig for opprettet samhandler $samhandlerId." }
    }

    private fun oppdaterSamhandler(samhandlerId: String) {
        val lagretAktør = aktørRepository.findByAktørIdent(samhandlerId)

        if (lagretAktør != null) {
            val oppdatertAktør = aktørService.hentAktørFraSamhandler(Ident(samhandlerId))
            aktørService.oppdaterAktør(lagretAktør, oppdatertAktør, lagretAktør.aktørIdent)
            LOGGER.info { "Oppdatert samhandler $samhandlerId." }
        }
    }

    private fun opphørSamhandler(samhandlerId: String) {
        LOGGER.info { "Sletting er ikke i bruk i aktørregisteret. Ingen handling nødvendig for sletting av samhandler $samhandlerId." }
    }
}
