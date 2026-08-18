package no.nav.bidrag.aktoerregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.transport.person.hendelse.Endringsmelding
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

@Service
class PersonHendelseService(private val objectMapper: ObjectMapper, private val aktørService: AktørService) {

    @Transactional
    fun behandleHendelse(hendelse: String) {
        secureLogger.info { "Behandler hendelse: $hendelse" }
        val endringsmelding = mapEndringsmelding(hendelse)

        endringsmelding.personidenter.forEach { ident ->
            val aktør = aktørService.hentAktørFraDatabase(Ident(ident)).first
            aktør?.let {
                LOGGER.debug { "Fant lagret aktør ${it.id}." }
                try {
                    val aktørFraPerson = aktørService.hentAktørFraPerson(Ident(ident))
                    if (aktør != aktørFraPerson) {
                        LOGGER.debug { "Lagret aktør ${it.id} er ulik ny aktør fra hendelse. Oppdaterer med nye verdier." }
                        aktørService.oppdaterAktør(aktør, aktørFraPerson, ident)
                    } else {
                        LOGGER.debug { "Lagret aktør ${it.id} er ikke ulik ny aktør fra hendelse. Går til neste hendelse." }
                    }
                    return
                } catch (e: AktørNotFoundException) {
                    secureLogger.error(e) { "Aktør med id: ${it.id} ikke funnet i bidrag-person!" }
                }
            }
        }
    }

    private fun mapEndringsmelding(hendelse: String): Endringsmelding = objectMapper.readValue(hendelse, Endringsmelding::class.java)
}
