package no.nav.bidrag.transport.behandling.vedtak

import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent

val VedtakHendelse.søknadsider
    get() =
        this.behandlingsreferanseListe
            ?.filter {
                it.kilde == BehandlingsrefKilde.BISYS_SØKNAD.name
            }?.map { it.referanse.toLong() } ?: emptyList()

val VedtakHendelse.søknadId
    get() =
        this.behandlingsreferanseListe
            ?.find {
                it.kilde == BehandlingsrefKilde.BISYS_SØKNAD.name
            }?.referanse
            ?.toLong()

val VedtakHendelse.søknadKlageRefId
    get() =
        this.behandlingsreferanseListe
            ?.find {
                it.kilde == BehandlingsrefKilde.BISYS_KLAGE_REF_SØKNAD.name
            }?.referanse
            ?.toLong()
val VedtakHendelse.behandlingId
    get() =
        this.behandlingsreferanseListe
            ?.find {
                it.kilde == BehandlingsrefKilde.BEHANDLING_ID.name
            }?.referanse
            ?.toLong()

fun VedtakHendelse.erDelvedtak() =
    this.stønadsendringListe?.any { se ->
        se.beslutning == Beslutningstype.DELVEDTAK
    } == true

fun VedtakHendelse.erFattetGjennomBidragBehandling() = behandlingId != null

fun VedtakHendelse.erFattetGjennomBidragAutomatiskJobb() = opprettetAv.contains("bidrag-automatisk-jobb")

val VedtakHendelse.saksnummer
    get(): String? =
        stønadsendringListe?.firstOrNull()?.sak?.verdi
            ?: engangsbeløpListe?.firstOrNull()?.sak?.verdi

val VedtakHendelse.typeBehandling get() =
    when {
        !stønadsendringListe.isNullOrEmpty() &&
            stønadsendringListe.first().type == Stønadstype.FORSKUDD -> TypeBehandling.FORSKUDD

        !engangsbeløpListe.isNullOrEmpty() &&
            engangsbeløpListe.first().type == Engangsbeløptype.SÆRBIDRAG -> TypeBehandling.SÆRBIDRAG

        else -> TypeBehandling.BIDRAG
    }

fun Stønadsendring.finnSøknadsbarnReferanse(grunnlagListe: List<GrunnlagDto>): String {
    val kravhaverIdent = kravhaver.verdi
    return grunnlagListe.hentPersonMedIdent(kravhaverIdent)!!.referanse
}
