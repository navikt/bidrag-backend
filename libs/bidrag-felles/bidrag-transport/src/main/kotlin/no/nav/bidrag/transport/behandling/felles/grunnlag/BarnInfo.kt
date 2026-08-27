package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode

data class BarnInfo(
    val navn: String,
    val fødselsnummer: Personident,
    val periode: ÅrMånedsperiode,
    val andelForsørget: Double? = null,
    val harSammeAdresse: Boolean? = null,
    val medIBeregning: Boolean? = null,
) : GrunnlagInnhold
