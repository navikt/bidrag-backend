package no.nav.bidrag.regnskap.dto.vedtak

import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import java.time.LocalDate

data class Hendelse(
    val type: String,
    val vedtakType: Vedtakstype,
    val kravhaverIdent: String?,
    val opprinneligKravhaverIdent: String?,
    val skyldnerIdent: String,
    val opprinneligSkyldnerIdent: String,
    val mottakerIdent: String,
    val opprinneligMottakerIdent: String,
    val sakId: String,
    val vedtakId: Int,
    val vedtakDato: LocalDate,
    val opprettetAv: String,
    val enhetsnummer: Enhetsnummer?,
    val eksternReferanse: String?,
    val utsattTilDato: LocalDate?,
    val referanse: String? = null,
    val omgjørVedtakId: Int? = null,
    val periodeListe: List<Periode>,
)
