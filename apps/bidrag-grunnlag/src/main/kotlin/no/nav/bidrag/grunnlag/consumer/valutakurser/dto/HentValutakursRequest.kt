package no.nav.bidrag.grunnlag.consumer.valutakurser.dto

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import java.time.LocalDate

data class HentValutakursRequest(val hentValutakursListe: List<HentValutakurs>)

data class HentValutakurs(val dato: LocalDate, val valutakode: Valutakode)
