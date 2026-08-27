package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakForStønad
import java.time.LocalDateTime
import java.time.YearMonth

data class ResultatFraVedtakGrunnlag(
    val vedtaksid: Int?,
    @JsonAlias("klagevedtak")
    val omgjøringsvedtak: Boolean = false,
    val beregnet: Boolean = false,
    // Om det skal opprettes paragraf 35c søknad for vedtakisden.
    // Brukes for etterfølgende vedtak når klagevedtak har beregningsperiode til opprinnelig vedtakstidspunkt
    val opprettParagraf35c: Boolean = false,
    val vedtakstidspunkt: LocalDateTime? = null,
    val vedtakstype: Vedtakstype? = null,
) : GrunnlagInnhold

data class VedtakOrkestreringDetaljerGrunnlag(
    @JsonAlias("klagevedtakId")
    val omgjøringsvedtakId: Int,
    val beregnTilDato: YearMonth,
    // Settes bare hvis det innkreves fra en annen dato enn vikrningstidspunktet
    val innkrevesFraDato: YearMonth? = null,
) : GrunnlagInnhold

data class EtterfølgendeManuelleVedtakGrunnlag(
    val vedtaksliste: List<VedtakForStønad> = emptyList(),
) : GrunnlagInnhold
