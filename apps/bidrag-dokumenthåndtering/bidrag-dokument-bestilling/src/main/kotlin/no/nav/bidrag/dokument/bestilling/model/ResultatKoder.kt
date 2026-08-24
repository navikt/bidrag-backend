package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvslag
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.tilBisysResultatkode
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype

object ResultatKoder {
    const val PRIVAT_AVTALE = "VX"
    const val INNVILGET_VEDTAK = "IV"
    const val UTENLANDSK_AVGJØRELSE = "UA"
    const val VEDTAK_VANLIG_INNKREVING = "V"
    const val FLERE_BESLUTNING_LINJER = "FB"
    const val FORSKUDD_50_PROSENT = "50"
    const val FORKSUDD_75_PROSENT = "75"
    const val FORSKUDD_100_PROSENT = "100"
    const val FORSKUDD_125_PROSENT = "125"
    const val FORSKUDD_200_PROSENT = "200"
    const val FORSKUDD_250_PROSENT = "250"
}

private val resultatkoderSomIkkeErStøttetAvBrev =
    listOf(
        Resultatkode.BIDRAGSPLIKTIG_ER_DØD,
        Resultatkode.IKKE_OMSORG_FOR_BARNET,
        Resultatkode.IKKE_DOKUMENTERT_SKOLEGANG,
        Resultatkode.AVSLUTTET_SKOLEGANG,
        Resultatkode.IKKE_INNKREVING_AV_BIDRAG,
        Resultatkode.AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE,
        Resultatkode.AVSLAG_PRIVAT_AVTALE_BIDRAG,
        Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE,
        Resultatkode.AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG,
        Resultatkode.PARTEN_BER_OM_OPPHØR,
        Resultatkode.BARNET_ANSES_Å_BO_SAMMEN_MED_BEGGE_FORELDRE,
        Resultatkode.PARTENE_BOR_SAMMEN,
    )

// "
// Koder som er støttet av brev for forskudd
// IF
// (BI_perForskVtak_resKd(SYS_TableRow) = 'AHI'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'AMD'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'AUT'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'AIR'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'AIO'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'ASA'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'ABA'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'AFT'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'AFU'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'ABI'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'ABE'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'AUY'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'A'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OHI'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OMD'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OUT'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OIR'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'OIO'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OSA'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OBA'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OFT'
// OR BI_perForskVtak_resKd(SYS_TableRow) = 'OFU'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'OBI'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'OBE'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'OUY'
// OR BI_perForskVtak_resKd (SYS_TableRow) = 'OH'
// OR BI_perForskVtak_resKd(SYS_TableRow) ='50'
// OR BI_perForskVtak_resKd(SYS_TableRow) ='75')
// THEN
// INCLUDE
// ENDIF
// "
fun Resultatkode.tilBisysResultatkodeForBrev(type: Vedtakstype) = if (resultatkoderSomIkkeErStøttetAvBrev.contains(this) && this.erAvslag()) {
    Resultatkode.AVSLAG.tilBisysResultatkode(type)
} else {
    this.tilBisysResultatkode(type)
}
