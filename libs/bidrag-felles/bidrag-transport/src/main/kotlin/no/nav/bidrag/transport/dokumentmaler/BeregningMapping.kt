package no.nav.bidrag.transport.dokumentmaler

import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.AldersjusteringDetaljerGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSamværsfradrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiDelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiSamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import java.math.BigDecimal

fun List<GrunnlagDto>.finnKopiDelberegningBidragspliktigesAndel(): KopiDelberegningBidragspliktigesAndel? {
    val delberegningBidragspliktigesAndel =
        find {
            it.type == Grunnlagstype.KOPI_DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL
        } ?: return null
    return delberegningBidragspliktigesAndel.innholdTilObjekt<KopiDelberegningBidragspliktigesAndel>()
}

fun List<GrunnlagDto>.finnAldersjusteringDetaljerGrunnlag(): AldersjusteringDetaljerGrunnlag? {
    val grunnlag =
        find {
            it.type == Grunnlagstype.ALDERSJUSTERING_DETALJER
        } ?: return null
    return grunnlag.innholdTilObjekt<AldersjusteringDetaljerGrunnlag>()
}

fun List<GrunnlagDto>.finnAldersjusteringDelberegningSamværsfradrag(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
): DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto
    .BidragPeriodeBeregningsdetaljer.NotatBeregningsdetaljerSamværsfradrag? {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
    val delberegningSamværsfradragGrunnlag =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull() ?: return null

    val samværsperiodeGrunnlag =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.KOPI_SAMVÆRSPERIODE,
            delberegningSamværsfradragGrunnlag.grunnlagsreferanseListe,
        ).firstOrNull() ?: return null

    val delberegningSamværsfradrag = delberegningSamværsfradragGrunnlag.innholdTilObjekt<DelberegningSamværsfradrag>()
    return DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto.BidragPeriodeBeregningsdetaljer
        .NotatBeregningsdetaljerSamværsfradrag(
            samværsfradrag = delberegningSamværsfradrag.beløp,
            samværsklasse = samværsperiodeGrunnlag.innholdTilObjekt<KopiSamværsperiodeGrunnlag>().samværsklasse,
            gjennomsnittligSamværPerMåned = BigDecimal.ZERO,
        )
}
