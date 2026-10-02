package no.nav.bidrag.sak.validering

import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.config.UnleashFeatures
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.sak.util.sammePerson
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period
import kotlin.text.isNotBlank

@Component
class BidragssakValidator(
    private val identConsumer: IdentConsumer,
    private val cachedKodeverkService: CachedKodeverkService,
    private val bidragPersonClient: BidragPersonClient,
) {

    data class Saksrolle(val id: Int?, val type: Rolletype, val ident: String?) {
        constructor(rolle: Rolle) : this(rolle.rolleId, rolle.rolleType, rolle.fødselsnummer?.takeIf { it.isNotBlank() })
        constructor(rolle: RolleDto) : this(null, rolle.type, rolle.fødselsnummer?.verdi?.takeIf { it.isNotBlank() })

        val erKjent get() = ident != null
    }

    fun valider(opprettSakRequest: OpprettSakRequest) {
        opprettSakRequest.land?.let {
            require(cachedKodeverkService.hentLandkoder().containsKey(opprettSakRequest.land)) {
                "Bidragssak forsøkt opprettet med ugyldig land: ${opprettSakRequest.land?.verdi}"
            }
        }

        validerForespurteRoller(opprettSakRequest.roller)

        loggHvisEierenhetOppretterSakUtenBarn(opprettSakRequest)

        val roller = opprettSakRequest.roller.map { Saksrolle(it) }
        validerMinstÉnKjentRolle(roller)
        validerÉnRollePerPerson(roller)
    }

    private fun loggHvisEierenhetOppretterSakUtenBarn(opprettSakRequest: OpprettSakRequest) {
        val erEierenhet = opprettSakRequest.arbeidsfordeling == Arbeidsfordeling.EIERENHET
        val harBarn = opprettSakRequest.roller.any { it.type == Rolletype.BARN }
        if (erEierenhet && !harBarn) {
            secureLogger.info {
                "Oppretter sak uten barn. Request: " +
                    "${commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(opprettSakRequest)}"
            }
        }
    }

    private fun validerRolle(rolle: RolleDto) {
        rolle.fødselsnummer?.let { fnr ->
            require(fnr.verdi.isNotBlank()) { "Fødselsnummer kan ikke være tom streng." }
        }

        require(rolle.type == Rolletype.BARN || !rolle.harRM()) {
            "Reell mottaker (RM) kan kun registreres på barn (BA)."
        }

        val fnr = rolle.fødselsnummer ?: return
        val personinfo = identConsumer.hentPersonInformasjon(fnr)
            ?: throw IllegalArgumentException("Person finnes ikke for rolle av type ${rolle.type}.")

        if (rolle.type == Rolletype.BARN) {
            validerAlderOgRm(fnr, rolle.harRM(), personinfo)
        }
    }

    private fun validerAlderOgRm(fnr: Personident, harRm: Boolean, personinfo: PersonDto) {
        if (harRm) return
        val fødselsdato = personinfo.fødselsdato ?: hentFødselsdato(fnr)
        if (fødselsdato != null) {
            require(beregnAlder(fødselsdato) < 18) {
                "Hvis barnet er myndig, må reell mottaker (RM) være satt."
            }
        }
    }

    private fun hentFødselsdato(fnr: Personident): LocalDate? = bidragPersonClient.hentFødselsdatoer(listOf(fnr))[fnr]

    fun validerForespurteRoller(roller: Collection<RolleDto>) {
        validerMaksEnBpOgBm(roller.map { Saksrolle(it) })
        roller.forEach { validerRolle(it) }
    }

    private fun validerMaksEnBpOgBm(roller: List<Saksrolle>) {
        require(roller.count { it.type == Rolletype.BIDRAGSMOTTAKER } <= 1) { "Kan ikke ha flere enn én bidragsmottaker (BM)." }
        require(roller.count { it.type == Rolletype.BIDRAGSPLIKTIG } <= 1) { "Kan ikke ha flere enn én bidragspliktig (BP)." }
    }

    private fun validerMinstÉnKjentRolle(roller: List<Saksrolle>) {
        require(roller.any { it.erKjent && erBpBmEllerBarn(it.type) }) { "Minst én person må ha en kjent rolle i saken." }
    }

    private fun validerÉnRollePerPerson(roller: List<Saksrolle>) {
        require(rollerMedSammePerson(roller).isEmpty()) { FEILMELDING_FLERE_ROLLER_FOR_PERSON }
    }

    private fun validerÉnRollePerPersonUnntattEksisterende(rollerFør: List<Saksrolle>, rollerEtter: List<Saksrolle>) {
        val eksisterendeIder = rollerFør.filter { it.erKjent }.map { it.id }.toSet()
        require(
            rollerMedSammePerson(rollerEtter).all { (rolle, annen) -> rolle.id in eksisterendeIder && annen.id in eksisterendeIder },
        ) { FEILMELDING_FLERE_ROLLER_FOR_PERSON }
    }

    private fun rollerMedSammePerson(roller: List<Saksrolle>): List<Pair<Saksrolle, Saksrolle>> {
        val rollerMedÉnRollePerPerson = roller.filter { it.erKjent && !erUnntattÉnRollePerPerson(it.type) }
        return rollerMedÉnRollePerPerson.flatMapIndexed { index, rolle ->
            rollerMedÉnRollePerPerson.drop(index + 1).filter { annen -> identConsumer.sammePerson(rolle.ident, annen.ident) }.map { rolle to it }
        }
    }

    private fun erUnntattÉnRollePerPerson(type: Rolletype): Boolean = when (type) {
        Rolletype.REELMOTTAKER, Rolletype.FEILREGISTRERT -> true
        Rolletype.BARN, Rolletype.BIDRAGSMOTTAKER, Rolletype.BIDRAGSPLIKTIG -> false
    }

    private fun erBpBmEllerBarn(type: Rolletype): Boolean = when (type) {
        Rolletype.BARN, Rolletype.BIDRAGSMOTTAKER, Rolletype.BIDRAGSPLIKTIG -> true
        Rolletype.REELMOTTAKER, Rolletype.FEILREGISTRERT -> false
    }

    fun validerRolleendring(rollerFør: List<Saksrolle>, lagredeRoller: Collection<Rolle>) {
        val rollerEtter = lagredeRoller.map { Saksrolle(it) }
        validerMaksEnBpOgBm(rollerEtter)
        validerMinstÉnKjentRolle(rollerEtter)
        validerKjenteRollerBeholdt(rollerFør, rollerEtter)
        validerÉnRollePerPersonVedEndring(rollerFør, rollerEtter)
    }

    private fun validerÉnRollePerPersonVedEndring(rollerFør: List<Saksrolle>, rollerEtter: List<Saksrolle>) {
        if (UnleashFeatures.TILLAT_EKSISTERENDE_DOBLE_SAKSROLLER.isEnabled) {
            validerÉnRollePerPersonUnntattEksisterende(rollerFør, rollerEtter)
        } else {
            validerÉnRollePerPerson(rollerEtter)
        }
    }

    private fun validerKjenteRollerBeholdt(rollerFør: List<Saksrolle>, rollerEtter: List<Saksrolle>) {
        rollerFør.filter { it.erKjent && !kanEndres(it.type) }.forEach { opprinnelig ->
            require(
                rollerEtter.any { rolle ->
                    rolle.id == opprinnelig.id &&
                        rolle.type == opprinnelig.type &&
                        identConsumer.sammePerson(rolle.ident, opprinnelig.ident)
                },
            ) { "En kjent rolle kan ikke fjernes eller endres." }
        }
    }

    private fun kanEndres(type: Rolletype): Boolean = when (type) {
        Rolletype.REELMOTTAKER, Rolletype.FEILREGISTRERT -> true
        Rolletype.BARN, Rolletype.BIDRAGSMOTTAKER, Rolletype.BIDRAGSPLIKTIG -> false
    }
}

private const val FEILMELDING_FLERE_ROLLER_FOR_PERSON = "En person kan bare ha én rolle i saken, unntatt RM og FR."

/**
 * Beregner alder basert på fødselsdato.
 */
fun beregnAlder(fødselsdato: LocalDate): Int = Period.between(fødselsdato, LocalDate.now()).years
