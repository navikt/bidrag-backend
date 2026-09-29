package no.nav.bidrag.person.hendelse.skedulering

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PublisereEndringsmeldinger(
    val bidragtopic: BidragKafkaMeldingsprodusent,
    val databasetjeneste: Databasetjeneste,
    val egenskaper: Egenskaper,
) {
    @Scheduled(cron = $$"${publisere_personhendelser.kjøreplan}")
    @SchedulerLock(
        name = "publisere_personhendelser",
        lockAtLeastFor = $$"${publisere_personhendelser.lås.min}",
        lockAtMostFor = $$"${publisere_personhendelser.lås.max}",
    )
    fun identifisereOgPublisere() {
        // Hente aktør med personidenter til til personer med nylige endringer i personopplysninger.
        // Uttrekket er begrenset i databasen for å holde minnebruken under kontroll.
        val aktørerPersonopplysninger =
            databasetjeneste.hentePubliseringsklareHendelser(
                egenskaper.generelt.maksAntallMeldingerSomSendesTilBidragTopicOmGangen,
            )
        log.info { "Fant ${aktørerPersonopplysninger.size} unike personer med nylige endringer i personopplysninger." }

        // Publisere melding til intern topic for samtlige personer med endringer
        aktørerPersonopplysninger.forEach { (aktør, opplysninger) ->
            bidragtopic.publisereEndringsmelding(aktør.aktorid, opplysninger.personidenter, opplysninger)
        }
    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}
