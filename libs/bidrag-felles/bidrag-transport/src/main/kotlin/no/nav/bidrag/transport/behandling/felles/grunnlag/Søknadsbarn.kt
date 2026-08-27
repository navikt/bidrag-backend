package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode

data class Søknadsbarn(
    val navn: String,
    val fødselsnummer: Personident,
    val periode: ÅrMånedsperiode,
    val andelForsørget: Double,
    val harSammeAdresse: Boolean,
    val medIBeregning: Boolean,
) : GrunnlagInnhold
