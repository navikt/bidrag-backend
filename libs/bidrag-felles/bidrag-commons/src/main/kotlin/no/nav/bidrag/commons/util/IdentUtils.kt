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

    fun hentNyesteIdent(
        @SjekkForNyIdent ident: Personident,
    ): Personident = if (ident.verdi == "NAV") Personident(NAV_TSS_IDENT) else ident

    fun hentAlleIdenter(ident: Personident) = identConsumer.hentAlleIdenter(ident.verdi)
}
