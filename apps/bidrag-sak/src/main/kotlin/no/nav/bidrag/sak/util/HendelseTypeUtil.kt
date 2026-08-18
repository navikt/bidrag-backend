package no.nav.bidrag.sak.util

import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.sak.domain.Hendelse

const val VEDTAK_LINK = "VEDTAK"

private val VEDTAK_ELLER_AVVISNING_TYPER =
    setOf(
        HendelseType.VEDTAK,
        HendelseType.VEDTAK_BARN_OVER_18,
        HendelseType.VEDTAK_BM,
        HendelseType.VEDTAK_BP,
        HendelseType.VEDTAK_FYLKESNEMDA,
        HendelseType.VEDTAK_KOMMUNE,
        HendelseType.VEDTAK_UTENLANDSKE_MYNDIGHETER,
        HendelseType.VEDTAK_VERGE,
        HendelseType.VEDTAK_FRA_BOST,
        HendelseType.VEDTAK_FTK,
        HendelseType.AVVIST,
    )

private val VEDTAK_TYPER =
    setOf(
        HendelseType.VEDTAK,
        HendelseType.VEDTAK_BARN_OVER_18,
        HendelseType.VEDTAK_BP,
        HendelseType.VEDTAK_BM,
        HendelseType.VEDTAK_VERGE,
        HendelseType.VEDTAK_FYLKESNEMDA,
        HendelseType.VEDTAK_KOMMUNE,
        HendelseType.VEDTAK_FRA_BOST,
        HendelseType.VEDTAK_FTK,
        HendelseType.VEDTAK_UTENLANDSKE_MYNDIGHETER,
        HendelseType.VEDTAK_MIDLERTIDIG,
        HendelseType.INDEKSREGULERT,
    )

private val REFUSJON_ELLER_INNKREVING_SØKNADSGRUPPER =
    setOf(
        SøknadGruppeKombinasjon.REFUSJON_BIDRAG,
        SøknadGruppeKombinasjon.INNKREVING,
    )

fun HendelseType?.erVedtakEllerAvvisning(): Boolean = this in VEDTAK_ELLER_AVVISNING_TYPER

fun HendelseType?.erVedtakstype(): Boolean = this in VEDTAK_TYPER

fun Hendelse.erKlageberettigetVedtak(): Boolean {
    val søknadsgruppe = grKombKode?.let(SøknadGruppeKombinasjon::fraKode)
    return type.erVedtakEllerAvvisning() && søknadsgruppe !in REFUSJON_ELLER_INNKREVING_SØKNADSGRUPPER
}

fun Hendelse.resultatIBisys(link: String?): Boolean = (!resultat.isNullOrBlank() && fraBbm && link != null) ||
    (link == VEDTAK_LINK && type.erVedtakstype() && søknad?.behandlingId == null)

fun Hendelse.erBisysVedtakOgErOverført(countOverfortVedtak: (søknadId: Int) -> Int): Boolean {
    val søknad = søknad ?: return false
    if (søknad.behandlingId != null) return false
    return søknad.id?.let { countOverfortVedtak(it) > 0 } ?: false
}
