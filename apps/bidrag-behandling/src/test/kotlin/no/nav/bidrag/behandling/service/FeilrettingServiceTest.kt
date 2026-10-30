package no.nav.bidrag.behandling.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Grunnlag
import no.nav.bidrag.behandling.database.datamodell.Inntekt
import no.nav.bidrag.behandling.database.datamodell.Inntektspost
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.grunnlag.SkattepliktigeInntekter
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.objectmapper
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SkattegrunnlagGrunnlagDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private const val OPPSLAG_SERVICE_KT = "no.nav.bidrag.behandling.service.OppslagServiceKt"
private const val ANNEN_PERSON_IDENT = "99999999999"
private const val HISTORISK_BM_IDENT = "88888888888"
private const val INNTEKTSPOST_KODE = "fastloenn"

class FeilrettingServiceTest {
    private val feilrettingService = FeilrettingService()

    private lateinit var behandling: Behandling
    private lateinit var bm: Rolle
    private val sistInnhentet = LocalDateTime.parse("2025-01-01T12:00:00")

    @BeforeEach
    fun setup() {
        mockkStatic(OPPSLAG_SERVICE_KT)
        every { hentNyesteIdentIgnorerForbidden(any()) } answers { Personident(firstArg()) }
        behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.grunnlag.clear()
        behandling.inntekter.clear()
        behandling.grunnlagSistInnhentet = sistInnhentet
        bm = behandling.bidragsmottaker!!
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(OPPSLAG_SERVICE_KT)
    }

    @Test
    fun `skal slette skattepliktige inntekter og avledede offentlige inntekter når grunnlag er hentet for annen person`() {
        // Given
        val rådata = behandling.leggTilSkattepliktigeInntekter(bm, ANNEN_PERSON_IDENT)
        val bearbeidet = behandling.leggTilGrunnlag(bm, Grunnlagsdatatype.SKATTEPLIKTIGE_INNTEKTER, erBearbeidet = true)
        val summerteMånedsinntekter = behandling.leggTilGrunnlag(bm, Grunnlagsdatatype.SUMMERTE_MÅNEDSINNTEKTER, erBearbeidet = true)
        val barnetilleggGrunnlag = behandling.leggTilGrunnlag(bm, Grunnlagsdatatype.BARNETILLEGG, erBearbeidet = false)
        val ainntekt = behandling.leggTilInntekt(bm, Inntektsrapportering.AINNTEKT_BEREGNET_12MND, Kilde.OFFENTLIG)
        val ligningsinntekt = behandling.leggTilInntekt(bm, Inntektsrapportering.LIGNINGSINNTEKT, Kilde.OFFENTLIG)
        val manuellInntekt = behandling.leggTilInntekt(bm, Inntektsrapportering.LØNN_MANUELT_BEREGNET, Kilde.MANUELL)
        val barnetillegg = behandling.leggTilInntekt(bm, Inntektsrapportering.BARNETILLEGG, Kilde.OFFENTLIG)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller shouldContainExactly listOf(bm.id!!)
        behandling.grunnlag shouldContainExactly setOf(barnetilleggGrunnlag)
        bm.grunnlag shouldContainExactly setOf(barnetilleggGrunnlag)
        listOf(rådata, bearbeidet, summerteMånedsinntekter).forEach { (it in behandling.grunnlag) shouldBe false }
        behandling.inntekter shouldContainExactlyInAnyOrder setOf(manuellInntekt, barnetillegg)
        ainntekt.inntektsposter.shouldBeEmpty()
        ligningsinntekt.inntektsposter.shouldBeEmpty()
        behandling.grunnlagSistInnhentet shouldBe null
    }

    @Test
    fun `skal slette når skattegrunnlag er hentet for annen person selv om ainntekter tilhører bidragsmottaker`() {
        // Given
        behandling.leggTilSkattepliktigeInntekter(bm, ainntektPersonId = bm.ident!!, skattegrunnlagPersonId = ANNEN_PERSON_IDENT)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller shouldContainExactly listOf(bm.id!!)
        behandling.grunnlag.shouldBeEmpty()
        behandling.grunnlagSistInnhentet shouldBe null
    }

    @Test
    fun `skal ikke slette når skattepliktige inntekter tilhører bidragsmottaker`() {
        // Given
        val rådata = behandling.leggTilSkattepliktigeInntekter(bm, bm.ident!!)
        val ainntekt = behandling.leggTilInntekt(bm, Inntektsrapportering.AINNTEKT_BEREGNET_12MND, Kilde.OFFENTLIG)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller.shouldBeEmpty()
        behandling.grunnlag shouldContainExactly setOf(rådata)
        behandling.inntekter shouldContainExactly setOf(ainntekt)
        ainntekt.inntektsposter.size shouldBe 1
        behandling.grunnlagSistInnhentet shouldBe sistInnhentet
    }

    @Test
    fun `skal ikke slette når personId er historisk ident for bidragsmottaker`() {
        // Given
        every { hentNyesteIdentIgnorerForbidden(HISTORISK_BM_IDENT) } returns Personident(bm.ident!!)
        val rådata = behandling.leggTilSkattepliktigeInntekter(bm, HISTORISK_BM_IDENT)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller.shouldBeEmpty()
        behandling.grunnlag shouldContainExactly setOf(rådata)
        behandling.grunnlagSistInnhentet shouldBe sistInnhentet
    }

    @Test
    fun `skal ikke slette skattepliktige inntekter for andre roller enn bidragsmottaker`() {
        // Given
        val bp = behandling.bidragspliktig!!
        val rådata = behandling.leggTilSkattepliktigeInntekter(bp, ANNEN_PERSON_IDENT)
        val ainntekt = behandling.leggTilInntekt(bp, Inntektsrapportering.AINNTEKT_BEREGNET_12MND, Kilde.OFFENTLIG)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller.shouldBeEmpty()
        behandling.grunnlag shouldContainExactly setOf(rådata)
        behandling.inntekter shouldContainExactly setOf(ainntekt)
        behandling.grunnlagSistInnhentet shouldBe sistInnhentet
    }

    @Test
    fun `skal bare slette for bidragsmottakeren som har feil grunnlag`() {
        // Given
        val bp = behandling.bidragspliktig!!
        behandling.leggTilSkattepliktigeInntekter(bm, ANNEN_PERSON_IDENT)
        val bpRådata = behandling.leggTilSkattepliktigeInntekter(bp, bp.ident!!)
        val bpAinntekt = behandling.leggTilInntekt(bp, Inntektsrapportering.AINNTEKT_BEREGNET_12MND, Kilde.OFFENTLIG)
        behandling.leggTilInntekt(bm, Inntektsrapportering.AINNTEKT_BEREGNET_12MND, Kilde.OFFENTLIG)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller shouldContainExactly listOf(bm.id!!)
        behandling.grunnlag shouldContainExactly setOf(bpRådata)
        behandling.inntekter shouldContainExactly setOf(bpAinntekt)
        bpAinntekt.inntektsposter.size shouldBe 1
    }

    @Test
    fun `skal ikke gjøre noe når vedtak er fattet`() {
        // Given
        behandling.vedtaksid = 1
        val rådata = behandling.leggTilSkattepliktigeInntekter(bm, ANNEN_PERSON_IDENT)

        // When
        val rettedeRoller = feilrettingService.slettSkattepliktigeInntekterHentetForAnnenPerson(behandling)

        // Then
        rettedeRoller.shouldBeEmpty()
        behandling.grunnlag shouldContainExactly setOf(rådata)
        behandling.grunnlagSistInnhentet shouldNotBe null
    }

    private fun Behandling.leggTilSkattepliktigeInntekter(
        rolle: Rolle,
        ainntektPersonId: String,
        skattegrunnlagPersonId: String = ainntektPersonId,
    ): Grunnlag {
        val periodeFra = LocalDate.parse("2024-01-01")
        val periodeTil = LocalDate.parse("2025-01-01")
        val data =
            SkattepliktigeInntekter(
                ainntekter = listOf(AinntektGrunnlagDto(ainntektPersonId, periodeFra, periodeTil, emptyList())),
                skattegrunnlag = listOf(SkattegrunnlagGrunnlagDto(skattegrunnlagPersonId, periodeFra, periodeTil, emptyList())),
            )
        return leggTilGrunnlag(rolle, Grunnlagsdatatype.SKATTEPLIKTIGE_INNTEKTER, erBearbeidet = false, data = objectmapper.writeValueAsString(data))
    }

    private fun Behandling.leggTilGrunnlag(
        rolle: Rolle,
        type: Grunnlagsdatatype,
        erBearbeidet: Boolean,
        data: String = "{}",
    ): Grunnlag = Grunnlag(
        behandling = this,
        type = type,
        erBearbeidet = erBearbeidet,
        data = data,
        innhentet = sistInnhentet,
        aktiv = sistInnhentet,
        rolle = rolle,
    ).also {
        grunnlag.add(it)
        rolle.grunnlag.add(it)
    }

    private fun Behandling.leggTilInntekt(
        rolle: Rolle,
        type: Inntektsrapportering,
        kilde: Kilde,
    ): Inntekt = Inntekt(
        type = type,
        belop = BigDecimal.TEN,
        datoFom = LocalDate.parse("2024-01-01"),
        datoTom = null,
        ident = rolle.ident,
        kilde = kilde,
        taMed = true,
        behandling = this,
        rolle = rolle,
    ).also { inntekt ->
        inntekt.inntektsposter.add(
            Inntektspost(beløp = BigDecimal.TEN, kode = INNTEKTSPOST_KODE, inntekt = inntekt, inntektstype = null),
        )
        inntekter.add(inntekt)
    }
}
