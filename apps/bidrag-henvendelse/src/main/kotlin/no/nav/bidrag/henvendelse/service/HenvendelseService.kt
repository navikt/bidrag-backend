package no.nav.bidrag.henvendelse.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.consumer.BidragPersonConsumer
import no.nav.bidrag.henvendelse.consumer.HenvendelseConsumer
import no.nav.bidrag.henvendelse.dto.HenvendelseDto
import no.nav.bidrag.henvendelse.dto.HenvendelserDto
import no.nav.bidrag.henvendelse.dto.Henvendelsestype
import no.nav.bidrag.henvendelse.dto.consumer.HenvendelseConsumerOutput
import no.nav.bidrag.henvendelse.dto.consumer.MeldingConsumerOutput
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class HenvendelseService(
    private val bidragPersonConsumer: BidragPersonConsumer,
    private val henvendelseConsumer: HenvendelseConsumer,
    private val tilgangskontroll: Tilgangskontroll,
) {
    /**
     * Henter henvendelsene for en person. Personer uten aktørid får tom liste uten at det
     * gjøres kall mot henvendelsesløsningen - samme oppførsel som BiSys.
     *
     * Tilgangen sjekkes før identvekslingen, slik at et oppslag saksbehandleren ikke har lov
     * til å gjøre heller ikke avslører om personen finnes i bidrag-person.
     */
    fun hentHenvendelser(personident: Personident): HenvendelserDto {
        tilgangskontroll.sjekkTilgangTilPerson(personident)

        val aktørid = bidragPersonConsumer.hentAktørid(personident)
        if (aktørid == null) {
            log.info { "Fant ingen aktørid for personen. Returnerer tom henvendelsesliste." }
            return HenvendelserDto(emptyList())
        }

        val fraKilden = henvendelseConsumer.hentHenvendelser(aktørid)
        val henvendelser = fraKilden.mapNotNull { it.tilHenvendelseDto() }
        loggAvvikIResponsen(fraKilden, antallMappet = henvendelser.size)
        return HenvendelserDto(henvendelser)
    }
}

/**
 * Avvik logges samlet én gang per kall, ikke per rad.
 *
 * Begge tilfellene her gjelder den enkelte henvendelsen, så en naiv `log.warn` inne i
 * mappingen ville gitt én linje per henvendelse per kall så lenge tilstanden varte - en ny
 * henvendelsestype fra kilden ville alene fylt loggen på tvers av all trafikk. Aggregert per
 * kall er volumet i stedet begrenset av hvor ofte en saksbehandler åpner brukeroversikten.
 *
 * Advarsel og ikke debug er et bevisst valg: debug er slått av i prod, og da er informasjonen
 * borte akkurat når man trenger den.
 */
private fun loggAvvikIResponsen(
    fraKilden: List<HenvendelseConsumerOutput>,
    antallMappet: Int,
) {
    val antallForkastet = fraKilden.size - antallMappet
    if (antallForkastet > 0) {
        log.warn { "Hoppet over $antallForkastet av ${fraKilden.size} henvendelser uten kjedeId." }
    }

    val ukjenteTyper = fraKilden
        .filter { it.henvendelseType.tilHenvendelsestype() == Henvendelsestype.UKJENT }
        .map { it.henvendelseType ?: "<mangler>" }
        .distinct()
    if (ukjenteTyper.isNotEmpty()) {
        log.warn { "Ukjente henvendelsestyper i responsen fra sf-henvendelse-api: $ukjenteTyper. Mappet til UKJENT." }
    }
}

/**
 * En henvendelse uten `kjedeId` kan ikke lenkes videre til Modia og er dermed ikke til nytte
 * i oversikten, så den forkastes framfor å vises som en død rad.
 */
private fun HenvendelseConsumerOutput.tilHenvendelseDto(): HenvendelseDto? = kjedeId?.let {
    HenvendelseDto(
        kjedeId = it,
        henvendelsestype = henvendelseType.tilHenvendelsestype(),
        tema = gjeldendeTema,
        temagruppe = gjeldendeTemagruppe,
        sisteMeldingSendt = meldinger.sisteSendtDato(),
    )
}

/**
 * Ren mapping uten sideeffekter - loggingen av ukjente verdier skjer i [loggAvvikIResponsen].
 *
 * Repoet håndterer ellers ukjente enum-verdier stille, via `@JsonEnumDefaultValue` og
 * `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` (se `Grunnlagstype` i bidrag-domene). Her
 * avvikes det bevisst: swaggeren lister `henvendelseType` som en lukket enum med tre verdier,
 * men en fjerde vil komme før eller senere, og da skal brukeroversikten vise den fram
 * framfor å skjule den.
 */
private fun String?.tilHenvendelsestype(): Henvendelsestype = when (this) {
    "CHAT" -> Henvendelsestype.CHAT
    "MELDINGSKJEDE" -> Henvendelsestype.MELDINGSKJEDE
    "SAMTALEREFERAT" -> Henvendelsestype.SAMTALEREFERAT
    else -> Henvendelsestype.UKJENT
}

private fun List<MeldingConsumerOutput>.sisteSendtDato() = mapNotNull { it.sendtDato }.maxOrNull()
