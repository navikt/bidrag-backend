package no.nav.bidrag.sak.dto

import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import java.time.LocalDateTime

data class SakshendelseDto(
    val hendelseId: String?,
    val opprettetTidspunkt: LocalDateTime,
    val enhet: Enhetsnummer,
    val søknadsgruppe: SøknadGruppeKombinasjon?,
    val type: HendelseType,
    val resultat: String?,
    val link: String?,
    val søknadsid: String?,
    /** Behandlings-id fra ny løsning (behandlingsflaten), dersom hendelsen tilhører en behandling der. */
    val behandlingsid: String?,
    /** Vedtaks-id fra ny løsning (bidrag-vedtak), dersom Bisys-hendelsen er overført dit. */
    val vedtaksid: String?,
    val erLukket: Boolean,
    val resultatIBisys: Boolean,
    val erBisysVedtakOgErOverført: Boolean,
    val erKlageberettigetVedtak: Boolean,
    val stonadType: Stønadstype?,
    val engangsbelopType: Engangsbeløptype?,
    val fraBbm: Boolean,
    val søktAv: SøktAvType? = null,
    val vedtakType: Vedtakstype? = null,
    val barnObjektNumre: List<String> = emptyList(),
    val erHovedsøknad: Boolean,
    val erDelAvFF: Boolean,
) {
    @Suppress("unused")
    val søknadsgruppeBeskrivelse: String?
        get() = if (erDelAvFF) {
            val postfiks = if (erHovedsøknad) {
                " (HFF)"
            } else if (type !in listOf(HendelseType.FORHOLDSMESSIG_FORDELING, HendelseType.FORHOLDSMESSIG_FORDELING_KLAGE)) {
                " (FF)"
            } else {
                ""
            }
            "${søknadsgruppe?.beskrivelse}$postfiks"
        } else {
            søknadsgruppe?.beskrivelse
        }

    @Suppress("unused")
    val typeBeskrivelse: String
        get() = type.beskrivelse

    @Suppress("unused")
    val resultatBeskrivelse: String?
        get() = resultat
}
