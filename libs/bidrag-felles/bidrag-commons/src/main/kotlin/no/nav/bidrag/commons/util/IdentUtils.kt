package no.nav.bidrag.commons.util

import no.nav.bidrag.domene.ident.Personident
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class IdentUtils(
    val identConsumer: IdentConsumer,
) {
    companion object {
        const val NAV_TSS_IDENT = "80000345435"
    }

    fun hentVisningsnavn(
        @SjekkForNyIdent ident: Personident,
    ): String? = identConsumer.hentPersonInformasjon(ident)?.visningsnavn

    fun hentFødselsdato(
        @SjekkForNyIdent ident: Personident,
    ): LocalDate? = identConsumer.hentPersonInformasjon(ident)?.fødselsdato

    // TODO(bidrag-person-feil): Gir innsendt ident når bidrag-person ikke finner personen (via @SjekkForNyIdent).
    //  Skal den heller returnere null, slik at kallestedet bestemmer?
    fun hentNyesteIdent(
        @SjekkForNyIdent ident: Personident,
    ): Personident = if (ident.verdi == "NAV") Personident(NAV_TSS_IDENT) else ident

    /**
     * Returnerer null når bidrag-person ikke finner personen. Andre feil kastes videre.
     */
    fun hentAlleIdenter(ident: Personident): List<String>? = identConsumer.hentAlleIdenter(ident.verdi)
}
