package no.nav.bidrag.admin.produksjonsoppfølging.domene

import no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities

data class Sak(
    val saksnr: String,
    val roller: no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities<no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolle, Long>,
    val søknadslinjer: no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities<no.nav.bidrag.admin.produksjonsoppfølging.domene.Søknadslinje, Long>,
    val beløp: no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities<no.nav.bidrag.admin.produksjonsoppfølging.domene.Beløp, Long>,
)
