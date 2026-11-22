package no.nav.bidrag.behandling.dto.v2.notat

const val NOTAT_OPPLYSNINGER_TITTEL_POSTFIKS = " - opplysninger fra offentlige registre"

/**
 * Hvilken del av saksbehandlingsnotatet som skal produseres. [KOMPLETT] er det samlede notatet,
 * mens [BEGRUNNELSE] og [OPPLYSNINGER] brukes når notatet splittes i to journalposter.
 */
enum class NotatDel(
    val referanseIdPostfiks: String = "",
    val tittelPostfiks: String = "",
) {
    KOMPLETT,
    BEGRUNNELSE(referanseIdPostfiks = "_begrunnelse"),
    OPPLYSNINGER(referanseIdPostfiks = "_opplysninger", tittelPostfiks = NOTAT_OPPLYSNINGER_TITTEL_POSTFIKS),
    ;

    companion object {
        val SPLITTEDE_DELER = listOf(BEGRUNNELSE, OPPLYSNINGER)
    }
}
