package no.nav.bidrag.transport.samhandler

data class FeltValideringsfeil(
    val feltnavn: String,
    var feilmelding: String,
)

fun MutableList<FeltValideringsfeil>.hent(feltnavn: String): FeltValideringsfeil? = this.find { it.feltnavn == feltnavn }

fun MutableList<FeltValideringsfeil>.leggTil(
    feltnavn: String,
    feilmelding: String,
) {
    if (this.hent(feltnavn) == null) {
        this.add(FeltValideringsfeil(feltnavn, feilmelding))
    } else {
        this.hent(feltnavn)!!.feilmelding = "${this.hent(feltnavn)!!.feilmelding}, $feilmelding"
    }
}

data class SamhandlerValideringsfeil(
    val duplikatSamhandler: List<DuplikatSamhandler>? = null,
    val ugyldigInput: List<FeltValideringsfeil>? = null,
)

data class DuplikatSamhandler(
    val feilmelding: String,
    val eksisterendeSamhandlerId: String,
    val felter: List<String> = emptyList(),
)
