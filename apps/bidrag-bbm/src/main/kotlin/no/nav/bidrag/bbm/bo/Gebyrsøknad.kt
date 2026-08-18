package no.nav.bidrag.bbm.bo

data class Gebyrsøknad(
    val søknadid: Long?,
    var rolleid: Long?,
    var referanse: String? = null,
)
