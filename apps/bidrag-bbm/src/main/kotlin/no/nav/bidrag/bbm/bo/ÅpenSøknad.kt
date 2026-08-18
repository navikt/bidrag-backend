package no.nav.bidrag.bbm.bo

import java.math.BigDecimal
import java.time.LocalDate

data class ÅpenSøknad(
    val behandlerenhet: String? = null,
    val saksnummer: String,
    val søknadsid: Long?,
    val refVedtaksid: Int?,
    val refSøknadsid: Long?,
    val blankettid: Long?,
    val søknadMottattDato: LocalDate,
    val søknadFomDato: LocalDate? = null,
    val søknadsgruppekode: String,
    val behandlingsid: String? = null,
    val søknadslinjeid: Long? = null,
    val personidentSøknadsbarn: String? = null,
    val innbetaltBeløp: BigDecimal? = null,
    val søknadsstatuskode: String,
    val gruppeKombinasjonskode: String,
    val referanseGebyr: String? = null,
)
