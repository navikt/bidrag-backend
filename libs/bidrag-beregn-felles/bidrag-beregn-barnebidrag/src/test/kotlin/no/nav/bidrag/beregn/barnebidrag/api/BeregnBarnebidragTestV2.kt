package no.nav.bidrag.beregn.barnebidrag.api

import io.mockk.mockkObject
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnBarnebidragService
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.barnebidrag.unleash.disableUnleashFeature
import no.nav.bidrag.beregn.barnebidrag.unleash.enableUnleashFeature
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgOppfostringsbidragBeregningException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningAndelAvBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumBidragTilFordeling
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
internal class BeregnBarnebidragTestV2 : FellesTest() {
    private lateinit var filnavnSøknadsbarn: String
    private var filnavnLøpendeBidrag: String = ""
    private var filnavnPrivatAvtale: String = ""
    private var filnavnValutakurs: String = ""
    private lateinit var beregningsperiode: ÅrMånedsperiode

    @Mock
    private lateinit var api: BeregnBarnebidragService
//    private lateinit var api: BeregnBarnebidragApi

    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        mockkObject(UnleashFeaturesProvider)
        api = BeregnBarnebidragService()
    }

    @Test
    @DisplayName("Barnebidrag - eksempel 1A - forholdsmessig fordeling 2 barn")
    fun testBarnebidrag_Eksempel01A() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel1A.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2020-08"), YearMonth.parse("2021-01"))

        utførBeregningerOgEvaluerResultatBarnebidrag()
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad med samværsfradrag - barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 10A - 2 søknadsbarn - 1 løpende bidrag med samværsfradrag - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel10A() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10A_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10A_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad - barn 3 - BM 2
    // Barn 1 har redusert evne i juli
    // Skal kaste exception
    @Test
    @DisplayName("Barnebidrag - eksempel 10B - 2 søknadsbarn - 1 løpende bidrag - ufullstendige grunnlag og redusert evne for søknadsbarn")
    fun testBarnebidrag_Eksempel10B() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10B_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10B_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))
        val exception = assertThrows<IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException> {
            utførBeregningerOgEvaluerResultatBarnebidrag()
        }

        val delberegningSumBidragTilFordelingResultatListe =
            exception.data
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(4) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(4) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-08"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-08"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[3].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad - barn 2 - BM 1 og barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 10C - 2 søknadsbarn - 2 løpende bidrag - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel10C() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10C_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10C_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad - barn 2 - BM 1 og barn 3 - BM 2
    // Barn 1 har redusert evne i juli
    // Skal kaste exception
    @Test
    @DisplayName("Barnebidrag - eksempel 10D - 2 søknadsbarn - 2 løpende bidrag - ufullstendige grunnlag og redusert evne for søknadsbarn")
    fun testBarnebidrag_Eksempel10D() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10D_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10D_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val exception = assertThrows<IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException> {
            utførBeregningerOgEvaluerResultatBarnebidrag()
        }

        val delberegningSumBidragTilFordelingResultatListe =
            exception.data
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(4) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(4) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-08"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-08"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[3].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad utland - barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 10E - 2 søknadsbarn - 1 løpende bidrag utland - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel10E() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10E_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10E_løpende_bidrag.json"
        filnavnValutakurs = "src/test/resources/testfiler/barnebidrag/barnebidragV2_valutakurs.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad uten samværsfradrag - barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 10F - 2 søknadsbarn - 1 løpende bidrag uten samværsfradrag - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel10F() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10F_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10F_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Komplette grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad (+ løpende bidrag) - barn 2 - BM 1
    // Revurderingssøknad - barn 3 - BM 2
    // Barn 1 har redusert evne i juli og virkningstidspunkt mai
    // Barn 2 har full evne og virkningstidspunkt september
    // Skal gå gjennom uten feil
    // Beregning for barn 1 skal returnere resultatperioder fra mai
    // Beregning for barn 2 skal returnere tomme perioder fra mai og resultatperioder fra september
    // Beregning for revurderingssøknad skal returnere 1 periode, med anbefaling om å ikke fatte vedtak
    @Test
    @DisplayName("Barnebidrag - eksempel 11A - 2 søknadsbarn - 1 løpende bidrag - komplette grunnlag og redusert evne for 1 søknadsbarn")
    fun testBarnebidrag_Eksempel11A() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel11A_søknadsbarn.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        barnebidragResultat.forEach { beregningResultat ->
            val andelAvBidragsevneResultatListe = beregningResultat.beregnetBarnebidragResultat.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE)
                .filter { it.gjelderBarnReferanse == beregningResultat.søknadsbarnreferanse }
                .map {
                    DelberegningAndelAvBidragsevne(
                        periode = it.innhold.periode,
                        andelAvSumBidragTilFordelingFaktor = it.innhold.andelAvSumBidragTilFordelingFaktor,
                        andelAvEvneBeløp = it.innhold.andelAvEvneBeløp,
                        bidragEtterFordeling = it.innhold.bidragEtterFordeling,
                        bruttoBidragJustertForEvneOg25Prosent = it.innhold.bruttoBidragJustertForEvneOg25Prosent,
                        harBPFullEvne = it.innhold.harBPFullEvne,
                    )
                }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(4) },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(4) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-08"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isFalse },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-08"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_02") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(1) },
                    {
                        assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(4) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-08"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-08"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Revurderingsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(1) },
                    { assertThat(beregningResultat.fatteVedtakAnbefalt).isFalse },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(5) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-08"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-08"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-12"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[4].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-12"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[4].harBPFullEvne).isTrue },
                )
            }
        }
    }

    // Komplette grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad (+ løpende bidrag) - barn 2 - BM 1
    // Revurderingssøknad - barn 3 - BM 2
    // Barn 1 har redusert evne i juli/august og virkningstidspunkt mai
    // Barn 2 har redusert evne i sep/okt/nov/des og virkningstidspunkt september
    // Skal gå gjennom uten feil
    // Beregning for barn 1 skal returnere resultatperioder fra mai
    // Beregning for barn 2 skal returnere tomme perioder fra mai og resultatperioder fra september
    // Beregning for revurderingssøknad skal returnere tomme perioder fra mai og resultatperioder fra desember
    @Test
    @DisplayName("Barnebidrag - eksempel 11B - 2 søknadsbarn - 1 løpende bidrag - komplette grunnlag og redusert evne for 2 søknadsbarn")
    fun testBarnebidrag_Eksempel11B() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel11B_søknadsbarn.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        barnebidragResultat.forEach { beregningResultat ->
            val andelAvBidragsevneResultatListe = beregningResultat.beregnetBarnebidragResultat.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE)
                .filter { it.gjelderBarnReferanse == beregningResultat.søknadsbarnreferanse }
                .map {
                    DelberegningAndelAvBidragsevne(
                        periode = it.innhold.periode,
                        andelAvSumBidragTilFordelingFaktor = it.innhold.andelAvSumBidragTilFordelingFaktor,
                        andelAvEvneBeløp = it.innhold.andelAvEvneBeløp,
                        bidragEtterFordeling = it.innhold.bidragEtterFordeling,
                        bruttoBidragJustertForEvneOg25Prosent = it.innhold.bruttoBidragJustertForEvneOg25Prosent,
                        harBPFullEvne = it.innhold.harBPFullEvne,
                    )
                }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(3) },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(3) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isFalse },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_02") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(1) },
                    {
                        assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(3) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isFalse },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Revurderingsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(1) },
                    {
                        assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-12"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(4) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-12"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-12"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                )
            }
        }
    }

    // Komplette grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad (+ løpende bidrag) - barn 2 - BM 1
    // Revurderingssøknad - barn 3 - BM 2
    // Barn 1 har redusert evne i juli/august og virkningstidspunkt mai
    // Barn 2 har redusert evne i september/oktober og virkningstidspunkt september
    // Skal gå gjennom uten feil
    // Beregning for barn 1 skal returnere resultatperioder fra mai
    // Beregning for barn 2 skal returnere tomme perioder fra mai og resultatperioder fra september
    // Beregning for revurderingssøknad skal returnere 1 periode, med anbefaling om å ikke fatte vedtak
    @Test
    @DisplayName("Barnebidrag - eksempel 11C - 2 søknadsbarn - 1 løpende bidrag - komplette grunnlag og redusert evne for 2 søknadsbarn")
    fun testBarnebidrag_Eksempel11C() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel11C_søknadsbarn.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        barnebidragResultat.forEach { beregningResultat ->
            val andelAvBidragsevneResultatListe = beregningResultat.beregnetBarnebidragResultat.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE)
                .filter { it.gjelderBarnReferanse == beregningResultat.søknadsbarnreferanse }
                .map {
                    DelberegningAndelAvBidragsevne(
                        periode = it.innhold.periode,
                        andelAvSumBidragTilFordelingFaktor = it.innhold.andelAvSumBidragTilFordelingFaktor,
                        andelAvEvneBeløp = it.innhold.andelAvEvneBeløp,
                        bidragEtterFordeling = it.innhold.bidragEtterFordeling,
                        bruttoBidragJustertForEvneOg25Prosent = it.innhold.bruttoBidragJustertForEvneOg25Prosent,
                        harBPFullEvne = it.innhold.harBPFullEvne,
                    )
                }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(4) },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(4) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isFalse },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-11"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-11"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_02") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(2) },
                    {
                        assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-11"),
                            ),
                        )
                    },
                    {
                        assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-11"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(4) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-11"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isFalse },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-11"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                )
            }

            if (beregningResultat.søknadsbarnreferanse == "Person_Revurderingsbarn_01") {
                assertAll(
                    { assertThat(beregningResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe).hasSize(1) },
                    { assertThat(beregningResultat.fatteVedtakAnbefalt).isFalse },
                    { assertThat(andelAvBidragsevneResultatListe).hasSize(5) },
                    {
                        assertThat(andelAvBidragsevneResultatListe[0].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-05"),
                                til = YearMonth.parse("2024-07"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[0].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[1].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-07"),
                                til = YearMonth.parse("2024-09"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[1].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[2].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-09"),
                                til = YearMonth.parse("2024-11"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[2].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[3].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-11"),
                                til = YearMonth.parse("2024-12"),
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[3].harBPFullEvne).isTrue },
                    {
                        assertThat(andelAvBidragsevneResultatListe[4].periode).isEqualTo(
                            ÅrMånedsperiode(
                                fom = YearMonth.parse("2024-12"),
                                til = null,
                            ),
                        )
                    },
                    { assertThat(andelAvBidragsevneResultatListe[4].harBPFullEvne).isFalse },
                )
            }
        }
    }

    // Ufullstendige grunnlag for privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale - barn 3 - BM 2 - uten samværsklasse
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 12A - 2 søknadsbarn - 1 privat avtale uten samværsklasse - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel12A() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12A_søknadsbarn.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12A_privat_avtale.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale - barn 3 - BM 2 - med samværsklasse
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 12B - 2 søknadsbarn - 1 privat avtale med samværsklasse - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel12B() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12B_søknadsbarn.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12B_privat_avtale.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale - barn 2 - BM 1 og barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 12C - 2 søknadsbarn - 2 private avtaler - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel12C() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12C_søknadsbarn.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12C_privat_avtale.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(2)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for privat avtale
    // Løpende bidrag som overlapper privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale - barn 3 - BM 2 - uten samværsklasse
    // Løpende bidrag - barn 3 - BM 2 - overlapper privat avtale
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName(
        "Barnebidrag - eksempel 12D - 2 søknadsbarn - 1 privat avtale uten samværsklasse - 1 løpende bidrag som overlapper " +
            "privat avtale - ufullstendige grunnlag og full evne",
    )
    fun testBarnebidrag_Eksempel12D() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12D_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12D_løpende_bidrag.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12D_privat_avtale.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale - barn 3 - BM 2 - med samværsklasse + utlandsbidrag
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 12E - 2 søknadsbarn - 1 privat avtale utland med samværsklasse - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel12E() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12E_søknadsbarn.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12E_privat_avtale.json"
        filnavnValutakurs = "src/test/resources/testfiler/barnebidrag/barnebidragV2_valutakurs.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for privat avtale
    // Løpende bidrag som overlapper privat avtale
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Privat avtale 18 år - barn 3 - BM 2 - uten samværsklasse
    // Løpende bidrag - barn 3 - BM 2 - overlapper privat avtale
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName(
        "Barnebidrag - eksempel 12F - 2 søknadsbarn - 1 privat avtale 18 år uten samværsklasse - 1 løpende bidrag som overlapper " +
            "privat avtale - ufullstendige grunnlag og full evne",
    )
    fun testBarnebidrag_Eksempel12F() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12F_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12F_løpende_bidrag.json"
        filnavnPrivatAvtale = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel12F_privat_avtale.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe)
                    .filteredOn { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende stønad - barn 3 - BM 2
    // Full evne i alle perioder
    // Skal gå gjennom uten feil
    @Test
    @DisplayName("Barnebidrag - eksempel 13A - 2 søknadsbarn - 1 løpende oppfostringsbidrag - ufullstendige grunnlag og full evne")
    fun testBarnebidrag_Eksempel13A() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel13A_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel13A_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))

        val barnebidragResultat = utførBeregningerOgEvaluerResultatBarnebidrag()

        val delberegningSumBidragTilFordelingResultatListe =
            barnebidragResultat
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(3) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(3) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    // Ufullstendige grunnlag for løpende bidrag
    // Ny søknad - barn 1 - BM 1
    // Endringssøknad - barn 2 - BM 1
    // Løpende oppfostringsbidrag - barn 3 - BM 2
    // Barn 1 har redusert evne i juli
    // Skal kaste exception
    @Test
    @DisplayName("Barnebidrag - eksempel 13B - 2 søknadsbarn - 1 løpende oppfostringsbidrag - ufullstendige grunnlag og redusert evne for søknadsbarn")
    fun testBarnebidrag_Eksempel13B() {
        filnavnSøknadsbarn = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel13B_søknadsbarn.json"
        filnavnLøpendeBidrag = "src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel13B_løpende_bidrag.json"
        beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-12"))
        val exception = assertThrows<IkkeFullBidragsevneOgOppfostringsbidragBeregningException> {
            utførBeregningerOgEvaluerResultatBarnebidrag()
        }

        val delberegningSumBidragTilFordelingResultatListe =
            exception.data
                .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
                .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
                .distinctBy { it.referanse }

        val delberegningSumBidragTilFordelingInnholdListe = delberegningSumBidragTilFordelingResultatListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            .map {
                DelberegningSumBidragTilFordeling(
                    periode = it.innhold.periode,
                    sumBidragTilFordeling = it.innhold.sumBidragTilFordeling,
                    sumPrioriterteBidragTilFordeling = it.innhold.sumPrioriterteBidragTilFordeling,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.innhold.erKompletteGrunnlagForAlleLøpendeBidrag,
                )
            }

        assertAll(
            { assertThat(delberegningSumBidragTilFordelingResultatListe).hasSize(4) },
            { assertThat(delberegningSumBidragTilFordelingInnholdListe).hasSize(4) },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[0].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-05"),
                        YearMonth.parse("2024-07"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[0].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[1].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-07"),
                        YearMonth.parse("2024-08"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[1].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[2].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-08"),
                        YearMonth.parse("2024-09"),
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).hasSize(2) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[2].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(1)
            },

            {
                assertThat(delberegningSumBidragTilFordelingInnholdListe[3].periode).isEqualTo(
                    ÅrMånedsperiode(
                        YearMonth.parse("2024-09"),
                        null,
                    ),
                )
            },
            { assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).hasSize(3) },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG_Person") }
                    .hasSize(1)
            },
            {
                assertThat(delberegningSumBidragTilFordelingResultatListe[3].grunnlagsreferanseListe).filteredOn { it.contains("BIDRAG_TIL_FORDELING_Person") }
                    .hasSize(2)
            },
        )
    }

    private fun utførBeregningerOgEvaluerResultatBarnebidrag(): List<BeregnetBarnebidragResultatV2> {
        val requestSøknadsbarn: List<BeregnGrunnlag> = lesFilOgByggRequestGenerisk(filnavnSøknadsbarn)
        val requestLøpendeBidrag: List<BeregnGrunnlag> =
            if (filnavnLøpendeBidrag.isEmpty()) emptyList() else lesFilOgByggRequestGenerisk(filnavnLøpendeBidrag)
        val requestPrivatAvtale: List<BeregnGrunnlag> =
            if (filnavnPrivatAvtale.isEmpty()) emptyList() else lesFilOgByggRequestGenerisk(filnavnPrivatAvtale)
        val requestValutakurs: List<GrunnlagDto> =
            if (filnavnValutakurs.isEmpty()) emptyList() else lesFilOgByggRequestGenerisk(filnavnValutakurs)

        val barnebidragResultat = api.beregnBarnebidragV2(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = requestSøknadsbarn,
            grunnlagLøpendeBidragListe = requestLøpendeBidrag,
            grunnlagPrivatAvtaleListe = requestPrivatAvtale,
            grunnlagValutakursListe = requestValutakurs,
        )

        println(commonObjectmapper.writeValueAsString(barnebidragResultat))

        barnebidragResultat.forEach { beregningResultat ->

            // TODO: Legge til asserts på beregningsresultater
//            val delberegningBidragTilFordelingResultatListe = beregningResultat.beregnetBarnebidragResultat.grunnlagListe
//                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordeling>(Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING)
//                .filter { it.gjelderBarnReferanse == beregningResultat.søknadsbarnreferanse }
//                .map {
//                    DelberegningBidragTilFordeling(
//                        periode = it.innhold.periode,
//                        bidragTilFordeling = it.innhold.bidragTilFordeling,
//                        uMinusNettoBarnetilleggBM = it.innhold.uMinusNettoBarnetilleggBM,
//                        bpAndelAvUMinusSamværsfradrag = it.innhold.bpAndelAvUMinusSamværsfradrag,
//                        nettoBidragEtterBarnetilleggBM = it.innhold.nettoBidragEtterBarnetilleggBM,
//                        bruttoBidragEtterBarnetilleggBM = it.innhold.bruttoBidragEtterBarnetilleggBM,
//                        erBidragJustertForNettoBarnetilleggBM = it.innhold.erBidragJustertForNettoBarnetilleggBM,
//                    )
//                }
//
//            if (beregningResultat.søknadsbarnreferanse == "Person_Søknadsbarn_01") {
//                assertAll(
//                    { assertThat(delberegningBidragTilFordelingResultatListe).hasSize(2) },
//                    {
//                        assertThat(delberegningBidragTilFordelingResultatListe[0].periode).isEqualTo(
//                            ÅrMånedsperiode(
//                                YearMonth.parse("2024-05"),
//                                YearMonth.parse("2024-07"),
//                            ),
//                        )
//                    },
//                    { assertThat(delberegningBidragTilFordelingResultatListe[0].bidragTilFordeling).isEqualTo(BigDecimal.valueOf(7433.85)) },
//                )
//            }

            val alleReferanser = hentAlleReferanser(beregningResultat.beregnetBarnebidragResultat.grunnlagListe)
            val alleRefererteReferanser = hentAlleRefererteReferanser(
                resultatGrunnlagListe = beregningResultat.beregnetBarnebidragResultat.grunnlagListe,
                barnebidragResultat = beregningResultat.beregnetBarnebidragResultat,
            )

            // Fjerner referanser som er "frittstående" (refereres ikke av noe objekt)
            val alleReferanserFiltrert = alleReferanser
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_Person") }
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_person") }
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_PERSON") }

            // Fjerner referanser som ikke er med i inputen til beregning eller som refererer til annet søknadsbarn
            val alleRefererteReferanserFiltrert = alleRefererteReferanser
                .filterNot { it.contains("innhentet_husstandsmedlem") }
                .filterNot { it.contains("innhentet_andre_barn") }
                .filterNot { it.contains("DELBEREGNING_BIDRAG_TIL_FORDELING") && !it.contains(beregningResultat.søknadsbarnreferanse) }
                .filterNot { it.contains("DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL") && !it.contains(beregningResultat.søknadsbarnreferanse) }
                .filterNot { it.contains("DELBEREGNING_SAMVÆRSFRADRAG") && !it.contains(beregningResultat.søknadsbarnreferanse) }
                .filterNot { it.contains("DELBEREGNING_UNDERHOLDSKOSTNAD") && !it.contains(beregningResultat.søknadsbarnreferanse) }

            assertThat(alleReferanserFiltrert).containsAll(alleRefererteReferanserFiltrert)
        }

        return barnebidragResultat
    }

    @Test
    fun `skal bruke ny logikk når feature er enablet`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        assertThat(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT.isEnabled).isTrue()
    }

    @Test
    fun `skal bruke gammel logikk når feature er disablet`() {
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        assertThat(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT.isEnabled).isFalse()
    }
}
