package no.nav.bidrag.sak.mapper.model

import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

data class RolleMedFødselsdato(
    val rolle: no.nav.bidrag.transport.sak.RolleDto,
    val fødselsdato: LocalDate?,
)

val RolleMedFødselsdato.type get() = rolle.type
val RolleMedFødselsdato.fødselsnummer get() = rolle.fødselsnummer
val RolleMedFødselsdato.rmSamhandlerId get() = rolle.rmSamhandlerId()
val RolleMedFødselsdato.rmFødselsnummer get() = rolle.rmFødselsnummer()
val RolleMedFødselsdato.harRM get() = rolle.harRM()
val RolleMedFødselsdato.reellMottaker get() =
    when {
        !rolle.rmFødselsnummer()?.verdi.isNullOrBlank() -> rolle.rmFødselsnummer()
        !rolle.rmSamhandlerId()?.verdi.isNullOrBlank() -> rolle.rmSamhandlerId()
        else -> null
    }

fun List<RolleMedFødselsdato>.tilFødselsdatoMap(): Map<Personident, LocalDate?> = this
    .mapNotNull { rolle ->
        rolle.fødselsnummer?.let { it to rolle.fødselsdato }
    }.toMap()
