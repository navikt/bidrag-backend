package no.nav.bidrag.person.hendelse.integrasjon.kontoregister

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.person.hendelse.prosess.Kontoendringsbehandler
import no.nav.person.endringsmelding.v1.Endringsmelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class Kontoendringsmottak(
    val kontoendringsbehandler: Kontoendringsbehandler,
) {
    @KafkaListener(
        groupId = "kontoregister-person-endringsmelding-v2.bidrag",
        topics = ["okonomi.kontoregister-person-endringsmelding.v2"],
        id = "bidrag-person-hendelse-kontoregister-person-endringsmelding-v2",
        idIsGroup = false,
    )
    fun listen(
        @Payload(required = false) endringsmelding: Endringsmelding?,
        cr: ConsumerRecord<String, Endringsmelding?>,
    ) {
        secureLogger.info {
            "Kontoregisterendringsmelding mottatt: Record key=${cr.key()}, value=${cr.value()}, value=${cr.offset()}"
        }

        if (harGyldigFormat(endringsmelding)) {
            kontoendringsbehandler.publisere(endringsmelding?.kontohaver.toString())
            secureLogger.info { "Kontoendring publisert for kontoeier ${endringsmelding?.kontohaver}".sanitizeForLog() }
        }
    }

    fun harGyldigFormat(endringsmelding: Endringsmelding?): Boolean {
        if (endringsmelding == null) {
            log.warn { "Innhold mangler i mottatt endringsmelding." }
            return false
        } else if (endringsmelding.kontohaver.isNullOrEmpty()) {
            log.warn { "Kontohaver mangler i mottatt endringsmelding." }
            return false
        } else if (!harGylidgFormat(endringsmelding.kontohaver.toString())) {
            log.warn { "Kontohavers personident har ikke gyldig format." }
            secureLogger.warn { "Kontohavers personident (${endringsmelding.kontohaver}) har ikke gyldig format." }
            return false
        }

        return true
    }

    fun harGylidgFormat(personident: String): Boolean = personident.isNotEmpty() && (personident.length == 11 || personident.length == 13)

    companion object {
        val log = KotlinLogging.logger {}
    }
}
