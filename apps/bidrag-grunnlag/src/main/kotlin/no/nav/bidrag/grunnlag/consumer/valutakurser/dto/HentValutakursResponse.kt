package no.nav.bidrag.grunnlag.consumer.valutakurser.dto

import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

data class HentValutakursResponse(val hentetValutakursListe: List<HentetValutakurs>)

data class HentetValutakurs(val periode: ÅrMånedsperiode, val valutakursSnitt: BigDecimal)
