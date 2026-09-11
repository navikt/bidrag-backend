package no.nav.bidrag.dokument.arkiv.kafka

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.dokument.arkiv.kafka.dto.OppgaveKafkaHendelse
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

@Service
class JsonMapperService(private val objectMapper: JsonMapper) {

    fun mapOppgaveHendelse(hendelse: String): OppgaveKafkaHendelse = try {
        objectMapper.readValue(hendelse, OppgaveKafkaHendelse::class.java)
    } finally {
        secureLogger.debug { "Leser hendelse: $hendelse" }
    }
}
