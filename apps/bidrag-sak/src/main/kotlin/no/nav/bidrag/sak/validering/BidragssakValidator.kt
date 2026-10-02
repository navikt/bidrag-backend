package no.nav.bidrag.sak.validering

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period
import kotlin.text.isNotBlank

@Component
class BidragssakValidator {
    data class Saksrolle(val id: Int?, val type: Rolletype, val ident: String?) {
        constructor(rolle: Rolle) : this(rolle.rolleId, rolle.rolleType, rolle.fødselsnummer?.takeIf { it.isNotBlank() })
        constructor(rolle: RolleDto) : this(null, rolle.type, rolle.fødselsnummer?.verdi?.takeIf { it.isNotBlank() })

        val erKjent get() = ident != null
    }

    fun valider(opprettSakRequest: OpprettSakRequest, grunnlag: Valideringsgrunnlag) {
        opprettSakRequest.land?.let {
            require(it in grunnlag.landkoder) {
                "Bidragssak forsøkt opprettet med ugyldig land: ${opprettSakRequest.land?.verdi}"
            }
        }

        validerForespurteRoller(opprettSakRequest.roller, grunnlag)

        loggHvisEierenhetOppretterSakUtenBarn(opprettSakRequest)

        val roller = opprettSakRequest.roller.map { Saksrolle(it) }
        validerMinstÉnKjentRolle(roller)
        validerÉnRollePerPerson(roller, grunnlag)
    }

    fun validerSaksopplysninger(oppdaterSakRequest: OppdaterSakRequest, grunnlag: Valideringsgrunnlag) {
        oppdaterSakRequest.landkode?.let {
            require(it in grunnlag.landkoder) {
                "Bidragssak ${oppdaterSakRequest.saksnummer} forsøkt oppdatert med ugyldig land: ${oppdaterSakRequest.landkode}"
            }
        }
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

    private fun validerRolle(rolle: RolleDto, grunnlag: Valideringsgrunnlag) {
        rolle.fødselsnummer?.let { fnr ->
            require(fnr.verdi.isNotBlank()) { "Fødselsnummer kan ikke være tom streng." }
        }

        require(rolle.type == Rolletype.BARN || !rolle.harRM()) {
            "Reell mottaker (RM) kan kun registreres på barn (BA)."
        }

        val fnr = rolle.fødselsnummer ?: return
        require(grunnlag.finnes(fnr)) { "Person finnes ikke for rolle av type ${rolle.type}." }

        if (rolle.type == Rolletype.BARN) {
            validerAlderOgRm(rolle.harRM(), grunnlag.fødselsdato(fnr))
        }
    }

    private fun validerAlderOgRm(harRm: Boolean, fødselsdato: LocalDate?) {
        if (harRm) return
        if (fødselsdato != null) {
            require(beregnAlder(fødselsdato) < 18) {
                "Hvis barnet er myndig, må reell mottaker (RM) være satt."
            }
        }
    }

    fun validerForespurteRoller(roller: Collection<RolleDto>, grunnlag: Valideringsgrunnlag) {
        validerMaksEnBpOgBm(roller.map { Saksrolle(it) })
        roller.forEach { validerRolle(it, grunnlag) }
    }

    private fun validerMaksEnBpOgBm(roller: List<Saksrolle>) {
        require(roller.count { it.type == Rolletype.BIDRAGSMOTTAKER } <= 1) { "Kan ikke ha flere enn én bidragsmottaker (BM)." }
        require(roller.count { it.type == Rolletype.BIDRAGSPLIKTIG } <= 1) { "Kan ikke ha flere enn én bidragspliktig (BP)." }
    }

    private fun validerMinstÉnKjentRolle(roller: List<Saksrolle>) {
        require(roller.any { it.erKjent && erBpBmEllerBarn(it.type) }) { "Minst én person må ha en kjent rolle i saken." }
    }

    private fun validerÉnRollePerPerson(roller: List<Saksrolle>, grunnlag: Valideringsgrunnlag) {
        require(rollerMedSammePerson(roller, grunnlag).isEmpty()) { FEILMELDING_FLERE_ROLLER_FOR_PERSON }
    }

    private fun validerÉnRollePerPersonUnntattEksisterende(
        rollerFør: List<Saksrolle>,
        rollerEtter: List<Saksrolle>,
        grunnlag: Valideringsgrunnlag,
    ) {
        val eksisterendeIder = rollerFør.filter { it.erKjent }.map { it.id }.toSet()
        require(
            rollerMedSammePerson(rollerEtter, grunnlag).all { (rolle, annen) -> rolle.id in eksisterendeIder && annen.id in eksisterendeIder },
        ) { FEILMELDING_FLERE_ROLLER_FOR_PERSON }
    }

    private fun rollerMedSammePerson(roller: List<Saksrolle>, grunnlag: Valideringsgrunnlag): List<Pair<Saksrolle, Saksrolle>> {
        val rollerMedÉnRollePerPerson = roller.filter { it.erKjent && !erUnntattÉnRollePerPerson(it.type) }
        return rollerMedÉnRollePerPerson.flatMapIndexed { index, rolle ->
            rollerMedÉnRollePerPerson.drop(index + 1).filter { annen -> grunnlag.sammePerson(rolle.ident, annen.ident) }.map { rolle to it }
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

    fun validerRolleendring(rollerFør: List<Saksrolle>, lagredeRoller: Collection<Rolle>, grunnlag: Valideringsgrunnlag) {
        val rollerEtter = lagredeRoller.map { Saksrolle(it) }
        validerMaksEnBpOgBm(rollerEtter)
        validerMinstÉnKjentRolle(rollerEtter)
        validerKjenteRollerBeholdt(rollerFør, rollerEtter, grunnlag)
        validerÉnRollePerPersonVedEndring(rollerFør, rollerEtter, grunnlag)
    }

    private fun validerÉnRollePerPersonVedEndring(rollerFør: List<Saksrolle>, rollerEtter: List<Saksrolle>, grunnlag: Valideringsgrunnlag) {
        if (grunnlag.tillatEksisterendeDobleRoller) {
            validerÉnRollePerPersonUnntattEksisterende(rollerFør, rollerEtter, grunnlag)
        } else {
            validerÉnRollePerPerson(rollerEtter, grunnlag)
        }
    }

    private fun validerKjenteRollerBeholdt(rollerFør: List<Saksrolle>, rollerEtter: List<Saksrolle>, grunnlag: Valideringsgrunnlag) {
        rollerFør.filter { it.erKjent && !kanEndres(it.type) }.forEach { opprinnelig ->
            require(
                rollerEtter.any { rolle ->
                    rolle.id == opprinnelig.id &&
                        rolle.type == opprinnelig.type &&
                        grunnlag.sammePerson(rolle.ident, opprinnelig.ident)
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
