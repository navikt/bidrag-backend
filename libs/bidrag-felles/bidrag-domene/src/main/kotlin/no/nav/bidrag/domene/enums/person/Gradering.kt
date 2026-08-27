@file:Suppress("unused")

package no.nav.bidrag.domene.enums.person

enum class Gradering(
    val diskresjonskode: Diskresjonskode?,
) {
    STRENGT_FORTROLIG(Diskresjonskode.SPSF),
    FORTROLIG(Diskresjonskode.SPFO),
    STRENGT_FORTROLIG_UTLAND(Diskresjonskode.P19),
    UGRADERT(null),
}
