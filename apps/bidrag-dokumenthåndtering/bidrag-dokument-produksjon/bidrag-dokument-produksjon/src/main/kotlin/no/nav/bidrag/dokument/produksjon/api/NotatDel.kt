package no.nav.bidrag.dokument.produksjon.api

/**
 * Hvilken del av notatet som skal produseres.
 * [innhold] er stien under `notat` i bidrag-dokumentmal: `notat/<innhold>/<dokumentmal>`.
 */
enum class NotatDel(
    val innhold: String?,
) {
    KOMPLETT(null),
    BEGRUNNELSE("begrunnelse"),
    OPPLYSNINGER("opplysninger"),
}
