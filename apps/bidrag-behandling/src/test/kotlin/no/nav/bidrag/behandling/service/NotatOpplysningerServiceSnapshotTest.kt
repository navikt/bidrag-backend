package no.nav.bidrag.behandling.service

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import net.bytebuddy.agent.ByteBuddyAgent
import no.nav.bidrag.behandling.consumer.BidragDokumentConsumer
import no.nav.bidrag.behandling.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Utgiftspost
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.MANUELL_INNTEKT_GJELDER_BARN_IDENT
import no.nav.bidrag.behandling.utils.testdata.erstattVariablerITestFil
import no.nav.bidrag.behandling.utils.testdata.identBarnIGrunnlagsrespons
import no.nav.bidrag.behandling.utils.testdata.leggTilBarnetillegg
import no.nav.bidrag.behandling.utils.testdata.leggTilBarnetilsyn
import no.nav.bidrag.behandling.utils.testdata.leggTilFaktiskTilsynsutgift
import no.nav.bidrag.behandling.utils.testdata.leggTilGebyrSøknad
import no.nav.bidrag.behandling.utils.testdata.leggTilNotat
import no.nav.bidrag.behandling.utils.testdata.leggTilSamvær
import no.nav.bidrag.behandling.utils.testdata.leggTilTillegsstønad
import no.nav.bidrag.behandling.utils.testdata.opprettAlleAktiveGrunnlagFraFil
import no.nav.bidrag.behandling.utils.testdata.opprettEvnevurderingResultat
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.behandling.utils.testdata.opprettSakForBehandling
import no.nav.bidrag.behandling.utils.testdata.testdataBarn1
import no.nav.bidrag.behandling.utils.testdata.testdataBarnBm
import no.nav.bidrag.behandling.utils.testdata.testdataHusstandsmedlem1
import no.nav.bidrag.behandling.utils.testdata.voksenPersonIBpsHusstand
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.særbidrag.Utgiftstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.NotatGrunnlag.NotatType
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

/**
 * Golden master-test for [NotatOpplysningerService.hentNotatOpplysningerForBehandling].
 *
 * Sammenligner serialisert notat mot lagrede snapshots i `src/test/resources/notat-snapshot`.
 * Kjør med `-DOPPDATER_SNAPSHOT=true` (eller miljøvariabel) for å oppdatere snapshots.
 * Ved avvik skrives faktisk resultat til `target/notat-snapshot-actual`.
 */
class NotatOpplysningerServiceSnapshotTest : CommonVedtakTilBehandlingTest() {
    lateinit var notatOpplysningerServiceSnapshot: NotatOpplysningerService

    @BeforeEach
    fun initSnapshotTest() {
        // Testdata og beregning bruker LocalDate.now()/YearMonth.now() (bl.a. beregnTil-dato og særbidrag-virkningstidspunkt).
        // Låser "nå" slik at snapshotene er stabile over tid.
        åpneJavaTimeForMockk()
        mockkStatic(LocalDate::class, LocalDateTime::class, YearMonth::class, Year::class)
        every { LocalDate.now() } returns FAST_DATO
        every { LocalDateTime.now() } returns FAST_DATO.atTime(12, 0)
        every { YearMonth.now() } returns YearMonth.from(FAST_DATO)
        every { Year.now() } returns Year.of(FAST_DATO.year)

        stubPersonConsumer()
        every { tilgangskontrollService.harTilgang(any(), any()) } returns true
        every { tilgangskontrollService.harBeskyttelse(any()) } returns false
        every { validerBehandlingService.kanBehandlesINyLøsning(any()) } returns null
        every { evnevurderingService.hentLøpendeBidragForBehandling(any()) } returns
            opprettEvnevurderingResultat(
                listOf(
                    testdataBarn1 to Stønadstype.BIDRAG,
                    testdataHusstandsmedlem1 to Stønadstype.BIDRAG,
                ),
            )
        notatOpplysningerServiceSnapshot =
            NotatOpplysningerService(
                behandlingService,
                beregningService,
                mockk<BidragDokumentProduksjonConsumer>(),
                mockk<BidragDokumentConsumer>(),
                dtomapper,
            )
    }

    @AfterEach
    fun ryddOpp() {
        unmockkStatic(LocalDate::class, LocalDateTime::class, YearMonth::class, Year::class)
    }

    @Test
    fun `skal lage notatopplysninger for innvilget forskudd lik snapshot`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true)
        behandling.leggTilNotat("Inntektsbegrunnelse kun i notat", NotatType.INNTEKT, behandling.bidragsmottaker)
        behandling.leggTilNotat("Virkningstidspunkt kun i notat", NotatType.VIRKNINGSTIDSPUNKT)
        behandling.leggTilNotat("Boforhold", NotatType.BOFORHOLD)
        behandling.grunnlag = opprettAlleAktiveGrunnlagFraFil(behandling, "grunnlagresponse.json")

        val notat = hentNotatOgSammenlignMedSnapshot(behandling, "forskudd_innvilget")

        notat.vedtak.resultat.shouldNotBeEmpty()
    }

    @Test
    fun `skal lage notatopplysninger for avslag forskudd lik snapshot`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true)
        behandling.avslag = Resultatkode.AVSLAG
        behandling.leggTilNotat("Inntektsbegrunnelse kun i notat", NotatType.INNTEKT, behandling.bidragsmottaker)
        behandling.leggTilNotat("Virkningstidspunkt kun i notat", NotatType.VIRKNINGSTIDSPUNKT)
        behandling.grunnlag = opprettAlleAktiveGrunnlagFraFil(behandling, "grunnlagresponse.json")

        val notat = hentNotatOgSammenlignMedSnapshot(behandling, "forskudd_avslag")

        notat.vedtak.resultat.shouldNotBeEmpty()
    }

    @Test
    fun `skal lage notatopplysninger for særbidrag med utgifter lik snapshot`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.SÆRBIDRAG)
        behandling.leggTilNotat("Notat inntekt BM", NotatType.INNTEKT, behandling.bidragsmottaker)
        behandling.leggTilNotat("Notat inntekt BP", NotatType.INNTEKT, behandling.bidragspliktig)
        behandling.leggTilNotat("Utgiftsbegrunnelse", NotatType.UTGIFTER)
        behandling.leggTilNotat("Boforhold", NotatType.BOFORHOLD)
        behandling.kategori = Særbidragskategori.KONFIRMASJON.name
        behandling.utgift!!.beløpDirekteBetaltAvBp = BigDecimal(500)
        behandling.utgift!!.maksGodkjentBeløp = BigDecimal(15000)
        behandling.utgift!!.maksGodkjentBeløpBegrunnelse = "Maks godkjent beløp"
        behandling.utgift!!.maksGodkjentBeløpTaMed = true
        behandling.utgift!!.utgiftsposter =
            mutableSetOf(
                Utgiftspost(
                    id = 1,
                    dato = FAST_DATO.minusMonths(3),
                    type = Utgiftstype.KONFIRMASJONSAVGIFT.name,
                    utgift = behandling.utgift!!,
                    kravbeløp = BigDecimal(15000),
                    godkjentBeløp = BigDecimal(5000),
                    kommentar = "Inneholder avgifter for alkohol og pynt",
                ),
                Utgiftspost(
                    id = 2,
                    dato = FAST_DATO.minusMonths(8),
                    type = Utgiftstype.KLÆR.name,
                    utgift = behandling.utgift!!,
                    kravbeløp = BigDecimal(10000),
                    godkjentBeløp = BigDecimal(10000),
                ),
                Utgiftspost(
                    id = 3,
                    dato = FAST_DATO.minusMonths(5),
                    type = Utgiftstype.SELSKAP.name,
                    utgift = behandling.utgift!!,
                    kravbeløp = BigDecimal(10000),
                    godkjentBeløp = BigDecimal(5000),
                    kommentar = "Inneholder utgifter til mat og drikke",
                ),
            )
        behandling.grunnlag = opprettAlleAktiveGrunnlagFraFil(behandling, "grunnlagresponse.json")

        val notat = hentNotatOgSammenlignMedSnapshot(behandling, "saerbidrag")

        notat.vedtak.resultat.shouldNotBeEmpty()
    }

    @Test
    fun `skal lage notatopplysninger for bidrag med samvær underhold og gebyr lik snapshot`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.bidragsmottaker!!.leggTilGebyrSøknad(behandling.soknadsid!!, "REFERANSE_BM_GEBYR")
        behandling.bidragspliktig!!.leggTilGebyrSøknad(behandling.soknadsid!!, "REFERANSE_BP_GEBYR")
        val virkningstidspunkt = behandling.virkningstidspunkt!!
        behandling.leggTilSamvær(ÅrMånedsperiode(virkningstidspunkt, virkningstidspunkt.plusMonths(1)), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, medId = true)
        behandling.leggTilSamvær(ÅrMånedsperiode(virkningstidspunkt.plusMonths(1), null), medId = true)
        behandling.leggTilTillegsstønad(ÅrMånedsperiode(virkningstidspunkt.plusMonths(4), null), medId = true)
        behandling.leggTilFaktiskTilsynsutgift(ÅrMånedsperiode(virkningstidspunkt.plusMonths(1), null), testdataHusstandsmedlem1, medId = true)
        behandling.leggTilFaktiskTilsynsutgift(ÅrMånedsperiode(virkningstidspunkt.plusMonths(1), null), testdataBarnBm, medId = true)
        behandling.leggTilFaktiskTilsynsutgift(ÅrMånedsperiode(virkningstidspunkt, null), medId = true)
        behandling.leggTilBarnetilsyn(ÅrMånedsperiode(virkningstidspunkt.plusMonths(1), null), generateId = true)
        behandling.leggTilBarnetilsyn(
            ÅrMånedsperiode(virkningstidspunkt, virkningstidspunkt.plusMonths(1)),
            generateId = true,
            tilsynstype = Tilsynstype.HELTID,
            under_skolealder = true,
            kilde = Kilde.OFFENTLIG,
        )
        behandling.leggTilBarnetillegg(testdataBarn1, behandling.bidragsmottaker!!, medId = true)
        behandling.leggTilBarnetillegg(testdataBarn1, behandling.bidragspliktig!!, medId = true)
        behandling.leggTilNotat("Inntektsbegrunnelse BM", NotatType.INNTEKT, behandling.bidragsmottaker)
        behandling.leggTilNotat("Virkningstidspunkt", NotatType.VIRKNINGSTIDSPUNKT, behandling.søknadsbarn.first())
        behandling.leggTilNotat("Boforhold", NotatType.BOFORHOLD, behandling.bidragspliktig)
        behandling.leggTilNotat("Samvær", NotatType.SAMVÆR, behandling.søknadsbarn.first())
        behandling.leggTilNotat("Underhold barn", NotatType.UNDERHOLDSKOSTNAD, behandling.søknadsbarn.first())
        behandling.leggTilNotat("Underhold andre barn", NotatType.UNDERHOLDSKOSTNAD, behandling.bidragsmottaker)
        behandling.grunnlag = opprettAlleAktiveGrunnlagFraFil(behandling, erstattVariablerITestFil("grunnlagresponse_bp_bm"))
        every { sakConsumer.hentSak(any()) } returns opprettSakForBehandling(behandling)

        val notat = hentNotatOgSammenlignMedSnapshot(behandling, "bidrag")

        notat.vedtak.resultat.shouldNotBeEmpty()
    }

    private fun hentNotatOgSammenlignMedSnapshot(
        behandling: Behandling,
        navn: String,
    ): VedtakNotatDto {
        every { behandlingService.hentBehandlingById(any()) } returns behandling
        val notat = notatOpplysningerServiceSnapshot.hentNotatOpplysningerForBehandling(behandling)
        val faktisk = snapshotMapper.writerWithDefaultPrettyPrinter().writeValueAsString(notat).normaliserLinjeskift().normaliserGenererteIdenter() + "\n"

        val snapshotFil = File(SNAPSHOT_MAPPE, "$navn.json")
        if (skalOppdatereSnapshot() || !snapshotFil.exists()) {
            snapshotFil.parentFile.mkdirs()
            snapshotFil.writeText(faktisk)
            return notat
        }

        val forventet = snapshotFil.readText().normaliserLinjeskift()
        if (faktisk != forventet) {
            File(FAKTISK_MAPPE, "$navn.json").apply {
                parentFile.mkdirs()
                writeText(faktisk)
            }
        }
        faktisk shouldBe forventet
        return notat
    }

    // mockkStatic på java.time krever at mockk kan kalle private metoder (f.eks. LocalDate.create) for ikke-mockede kall.
    // Åpner java.time for mockk sin modul under kjøring i stedet for å endre JVM-argumentene til surefire.
    private fun åpneJavaTimeForMockk() {
        val moduler =
            setOf(
                javaClass.classLoader.unnamedModule,
                Class.forName("io.mockk.proxy.jvm.advice.MethodCall").module,
            )
        ByteBuddyAgent.install().redefineModule(
            LocalDate::class.java.module,
            emptySet(),
            emptyMap(),
            mapOf("java.time" to moduler),
            emptySet(),
            emptyMap(),
        )
    }

    private fun String.normaliserLinjeskift() = replace("\r\n", "\n")

    // Enkelte testpersoner får tilfeldig genererte fødselsnummer ved oppstart (genererFødselsnummer()).
    // Erstatter dem med faste plassholdere slik at snapshotene er like mellom kjøringer.
    private fun String.normaliserGenererteIdenter() = mapOf(
        testdataHusstandsmedlem1.ident to "<HUSSTANDSMEDLEM1_IDENT>",
        voksenPersonIBpsHusstand.personident to "<VOKSEN_I_BPS_HUSSTAND_IDENT>",
        identBarnIGrunnlagsrespons to "<BARN_I_GRUNNLAGSRESPONS_IDENT>",
        MANUELL_INNTEKT_GJELDER_BARN_IDENT to "<MANUELL_INNTEKT_GJELDER_BARN_IDENT>",
    ).entries.fold(this) { json, (ident, plassholder) -> json.replace(ident, plassholder) }

    private fun skalOppdatereSnapshot() = (System.getProperty(OPPDATER_SNAPSHOT) ?: System.getenv(OPPDATER_SNAPSHOT)).toBoolean()

    companion object {
        private const val OPPDATER_SNAPSHOT = "OPPDATER_SNAPSHOT"
        private const val SNAPSHOT_MAPPE = "src/test/resources/notat-snapshot"
        private const val FAKTISK_MAPPE = "target/notat-snapshot-actual"
        private val FAST_DATO: LocalDate = LocalDate.of(2025, 5, 15)

        private val snapshotMapper =
            commonObjectmapper
                .copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    }
}
