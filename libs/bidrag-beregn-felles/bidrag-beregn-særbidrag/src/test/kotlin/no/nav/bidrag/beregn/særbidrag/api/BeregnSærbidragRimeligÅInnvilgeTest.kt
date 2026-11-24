package no.nav.bidrag.beregn.særbidrag.api

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.særbidrag.service.BeregnSærbidragService
import no.nav.bidrag.beregn.særbidrag.testdata.SjablonApiStub
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.særbidrag.BeregnetSærbidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.SjablonSjablontallPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SærbidragRimeligÅInnvilgeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths

internal class BeregnSærbidragRimeligÅInnvilgeTest {
    private val filnavn = "src/test/resources/testfiler/særbidrag_eksempel_barnet_er_selvforsørget.json"
    private val referanseRimeligÅInnvilge = "særbidrag_rimelig_å_innvilge"
    private lateinit var beregnSærbidragService: BeregnSærbidragService

    @BeforeEach
    fun initMock() {
        SjablonApiStub().settOppSjablonStub()
        beregnSærbidragService = BeregnSærbidragService()
    }

    @Test
    @DisplayName("barnet er selvforsørget og saksbehandler har vurdert at det er rimelig å innvilge - BPs andel regnes ut")
    fun skalBeregneBPsAndelNårDetErRimeligÅInnvilge() {
        val request = lesFilOgByggRequest(rimeligÅInnvilge = true)

        val resultat = beregnSærbidragService.beregn(request, Vedtakstype.FASTSETTELSE)

        val andel = resultat.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragspliktigesAndel>(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL)
            .single()
        val forskuddssats = resultat.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SjablonSjablontallPeriode>(Grunnlagstype.SJABLON_SJABLONTALL)
            .map { it.innhold }
            .single { it.sjablon == SjablonTallNavn.FORSKUDDSSATS_BELØP }
            .verdi

        assertAll(
            { assertThat(resultat.beregnetSærbidragPeriodeListe).hasSize(1) },
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.resultatkode).isEqualTo(Resultatkode.SÆRBIDRAG_INNVILGET) },
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.beløp).isGreaterThan(BigDecimal.ZERO) },
            { assertThat(andel.innhold.barnetErSelvforsørget).isFalse() },
            { assertThat(andel.innhold.andelBeløp).isGreaterThan(BigDecimal.ZERO) },
            // Barnets inntekt over 30 ganger forskuddssats regnes med
            {
                assertThat(andel.innhold.barnEndeligInntekt)
                    .isEqualByComparingTo(BigDecimal.valueOf(200000) - forskuddssats.multiply(BigDecimal.valueOf(30)))
            },
            { assertThat(andel.grunnlag.grunnlagsreferanseListe).contains(referanseRimeligÅInnvilge) },
            { assertThat(resultat.grunnlagListe.map { it.referanse }).contains(referanseRimeligÅInnvilge) },
        )
    }

    @Test
    @DisplayName("barnet er selvforsørget og saksbehandler har vurdert at det ikke er rimelig å innvilge - avslag")
    fun skalGiAvslagNårDetIkkeErRimeligÅInnvilge() {
        val request = lesFilOgByggRequest(rimeligÅInnvilge = false)

        val resultat = beregnSærbidragService.beregn(request, Vedtakstype.FASTSETTELSE)

        val andel = resultat.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragspliktigesAndel>(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL)
            .single()

        assertAll(
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.resultatkode).isEqualTo(Resultatkode.BARNET_ER_SELVFORSØRGET) },
            { assertThat(andel.innhold.barnetErSelvforsørget).isTrue() },
            { assertThat(andel.innhold.andelBeløp).isEqualByComparingTo(BigDecimal.ZERO) },
            { assertThat(andel.grunnlag.grunnlagsreferanseListe).doesNotContain(referanseRimeligÅInnvilge) },
        )
    }

    @Test
    @DisplayName("barnets inntekt er lik 100 ganger forskuddssats - barnet er selvforsørget uten vurdering")
    fun skalGiAvslagNårBarnetsInntektErLikGrensenUtenVurdering() {
        val grense = forskuddssats().multiply(BigDecimal.valueOf(100))

        val resultat = beregnSærbidragService.beregn(lesFilOgByggRequest(rimeligÅInnvilge = null, barnetsInntekt = grense), Vedtakstype.FASTSETTELSE)

        assertAll(
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.resultatkode).isEqualTo(Resultatkode.BARNET_ER_SELVFORSØRGET) },
            { assertThat(bpsAndel(resultat).innhold.barnetErSelvforsørget).isTrue() },
        )
    }

    @Test
    @DisplayName("barnets inntekt er lik 100 ganger forskuddssats og det er rimelig å innvilge - vurderingen brukes")
    fun skalBrukeVurderingenNårBarnetsInntektErLikGrensen() {
        val grense = forskuddssats().multiply(BigDecimal.valueOf(100))

        val resultat = beregnSærbidragService.beregn(lesFilOgByggRequest(rimeligÅInnvilge = true, barnetsInntekt = grense), Vedtakstype.FASTSETTELSE)

        val andel = bpsAndel(resultat)
        assertAll(
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.resultatkode).isEqualTo(Resultatkode.SÆRBIDRAG_INNVILGET) },
            { assertThat(andel.innhold.barnetErSelvforsørget).isFalse() },
            { assertThat(andel.grunnlag.grunnlagsreferanseListe).contains(referanseRimeligÅInnvilge) },
        )
    }

    @Test
    @DisplayName("barnets inntekt er rett under 100 ganger forskuddssats - vurderingen brukes ikke")
    fun skalIkkeBrukeVurderingenNårBarnetsInntektErUnderGrensen() {
        val sats = forskuddssats()
        val underGrensen = sats.multiply(BigDecimal.valueOf(100)) - BigDecimal.ONE

        val resultat = beregnSærbidragService.beregn(lesFilOgByggRequest(rimeligÅInnvilge = true, barnetsInntekt = underGrensen), Vedtakstype.FASTSETTELSE)

        val andel = bpsAndel(resultat)
        assertAll(
            { assertThat(resultat.beregnetSærbidragPeriodeListe[0].resultat.resultatkode).isNotEqualTo(Resultatkode.BARNET_ER_SELVFORSØRGET) },
            { assertThat(andel.innhold.barnetErSelvforsørget).isFalse() },
            { assertThat(andel.grunnlag.grunnlagsreferanseListe).doesNotContain(referanseRimeligÅInnvilge) },
            {
                assertThat(andel.innhold.barnEndeligInntekt)
                    .isEqualByComparingTo(underGrensen - sats.multiply(BigDecimal.valueOf(30)))
            },
        )
    }

    private fun forskuddssats(): BigDecimal = beregnSærbidragService
        .beregn(lesFilOgByggRequest(rimeligÅInnvilge = null), Vedtakstype.FASTSETTELSE)
        .grunnlagListe
        .filtrerOgKonverterBasertPåEgenReferanse<SjablonSjablontallPeriode>(Grunnlagstype.SJABLON_SJABLONTALL)
        .map { it.innhold }
        .single { it.sjablon == SjablonTallNavn.FORSKUDDSSATS_BELØP }
        .verdi

    private fun bpsAndel(resultat: BeregnetSærbidragResultat) = resultat.grunnlagListe
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragspliktigesAndel>(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL)
        .single()

    private fun lesFilOgByggRequest(rimeligÅInnvilge: Boolean?, barnetsInntekt: BigDecimal? = null): BeregnGrunnlag {
        var json = Files.readString(Paths.get(filnavn))
        listOf("{FNR_SB}", "{FNR_BM}", "{FNR_BP}", "{FNR_HM1}", "{FNR_HM2}", "{FNR_HM3}", "{FNR_BABP}").forEach {
            json = json.replace(it, genererFødselsnummer())
        }
        if (barnetsInntekt != null) {
            json = json.replace("\"beløp\": 200000", "\"beløp\": ${barnetsInntekt.toPlainString()}")
        }
        val request = commonObjectmapper.readValue(json, BeregnGrunnlag::class.java)
        if (rimeligÅInnvilge == null) return request
        return request.copy(
            grunnlagListe = request.grunnlagListe +
                GrunnlagDto(
                    referanse = referanseRimeligÅInnvilge,
                    type = Grunnlagstype.SÆRBIDRAG_RIMELIG_Å_INNVILGE,
                    innhold = POJONode(SærbidragRimeligÅInnvilgeGrunnlag(rimeligÅInnvilge = rimeligÅInnvilge, begrunnelse = "Vurdering")),
                ),
        )
    }
}
