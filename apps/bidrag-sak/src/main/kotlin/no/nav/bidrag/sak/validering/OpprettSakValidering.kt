package no.nav.bidrag.sak.validering

import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period
import kotlin.text.isNotBlank

@Component
class OpprettSakValidator(
    private val identConsumer: IdentConsumer,
    private val cachedKodeverkService: CachedKodeverkService,
) {
    fun valider(opprettSakRequest: OpprettSakRequest) {
        opprettSakRequest.land?.let {
            require(cachedKodeverkService.hentLandkoder().containsKey(opprettSakRequest.land)) {
                "Bidragssak forsøkt opprettet med ugyldig land: ${opprettSakRequest.land?.verdi}"
            }
        }

        val antallBm = opprettSakRequest.roller.count { it.type == Rolletype.BIDRAGSMOTTAKER }
        require(antallBm <= 1) { "Kan ikke ha flere enn én bidragsmottaker (BM)." }

        val antallBp = opprettSakRequest.roller.count { it.type == Rolletype.BIDRAGSPLIKTIG }
        require(antallBp <= 1) { "Kan ikke ha flere enn én bidragspliktig (BP)." }

        if (opprettSakRequest.arbeidsfordeling == Arbeidsfordeling.EIERENHET) {
            val antallBarn = opprettSakRequest.roller.count { it.type == Rolletype.BARN }
            if (antallBarn ==
                0
            ) {
                secureLogger.info {
                    "Oppretter sak uten barn. Request: " +
                        "${commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(opprettSakRequest)}"
                }
            }
//            require(antallBarn > 0) { "Minst ett barn må være tilknyttet saken." }
        }

        val bmErOppgitt =
            opprettSakRequest.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER } != null

        if (!bmErOppgitt) {
            val alleBarnHarRm =
                opprettSakRequest.roller
                    .filter { it.type == Rolletype.BARN }
                    .all { it.harRM() }

            require(alleBarnHarRm) {
                "Når bidragsmottaker (BM) mangler, må alle barn (BA) ha reell mottaker (RM)."
            }
        }

        opprettSakRequest.roller.forEach { validerRolle(it) }
    }

    fun validerRolle(rolle: RolleDto) {
        rolle.fødselsnummer?.let { fnr ->
            require(fnr.verdi.isNotBlank()) { "Fødselsnummer kan ikke være tom streng." }
        }

        require(rolle.type == Rolletype.BARN || !rolle.harRM()) {
            "Reell mottaker (RM) kan kun registreres på barn (BA)."
        }

        val fnr =
            rolle.fødselsnummer
                ?.verdi
                ?.takeIf { it.isNotBlank() }
                ?.let { Personident(it) }
        val personinfo: PersonDto? =
            fnr?.let {
                identConsumer.hentPersonInformasjon(it)
                    ?: throw IllegalArgumentException("Person finnes ikke for rolle av type ${rolle.type}.")
            }

        // myndig barn -> må ha RM
        if (rolle.type == Rolletype.BARN && fnr != null) {
            val alder: Int? =
                runCatching {
                    val fødselsdato =
                        personinfo?.fødselsdato
                            ?: throw IllegalArgumentException("Mangler fødselsdato fra ident-oppslag.")

                    beregnAlder(fødselsdato)
                }.getOrNull()

            if (alder != null && alder >= 18) {
                require(rolle.harRM()) {
                    "Hvis barnet er myndig, må reell mottaker (RM) være satt."
                }
            }
        }
    }
}

/**
 * Beregner alder basert på fødselsdato.
 */
fun beregnAlder(fødselsdato: LocalDate): Int = Period.between(fødselsdato, LocalDate.now()).years
