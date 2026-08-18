package no.nav.bidrag.grunnlag.consumer.familiebasak.api

data class TilleggsstønadRequest(val ident: String)

data class TilleggsstønadResponse(val harInnvilgetVedtak: Boolean)
