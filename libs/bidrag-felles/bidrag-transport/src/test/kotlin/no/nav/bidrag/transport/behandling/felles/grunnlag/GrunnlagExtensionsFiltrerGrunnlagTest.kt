package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import kotlin.test.Test

class GrunnlagExtensionsFiltrerGrunnlagTest {
    private val bmRef1: Grunnlagsreferanse = "BM1"
    private val bmRef2: Grunnlagsreferanse = "BM2"
    private val bpRef1: Grunnlagsreferanse = "BP1"
    private val bpRef2: Grunnlagsreferanse = "BP2"
    private val barnRef1: Grunnlagsreferanse = "BARN1"
    private val barnRef2: Grunnlagsreferanse = "BARN2"

    @Test
    fun `returnerer true når gjelderReferanse er riktig BM og gjelderBarnReferanse er riktig barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bmRef1,
                gjelderBarnReferanse = barnRef1,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer true når gjelderReferanse er riktig BM og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bmRef1,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer false når gjelderReferanse er feil BM og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bmRef2,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe false
    }

    @Test
    fun `returnerer true når gjelderReferanse er riktig BP og gjelderBarnReferanse er riktig barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bpRef1,
                gjelderBarnReferanse = barnRef1,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer true når gjelderReferanse er riktig BP og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bpRef1,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer false når gjelderReferanse er feil BP og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bpRef2,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe false
    }

    @Test
    fun `returnerer true når gjelderReferanse er riktig barn og gjelderBarnReferanse er riktig barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = barnRef1,
                gjelderBarnReferanse = barnRef1,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer true når gjelderReferanse er riktig barn og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = barnRef1,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer false når gjelderReferanse er feil barn og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = barnRef2,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe false
    }

    @Test
    fun `returnerer true når gjelderReferanse er null og gjelderBarnReferanse er riktig barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = null,
                gjelderBarnReferanse = barnRef1,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer true når gjelderReferanse er null og gjelderBarnReferanse er null`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = null,
                gjelderBarnReferanse = null,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }

    @Test
    fun `returnerer false når gjelderReferanse er null og gjelderBarnReferanse er feil barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = null,
                gjelderBarnReferanse = barnRef2,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe false
    }

    @Test
    fun `returnerer true når type er BOSTATUS_PERIODE, gjelderReferanse er riktig BP og gjelderBarnReferanse er feil barn`() {
        val dto =
            GrunnlagDto(
                referanse = "Ref",
                type = Grunnlagstype.BOSTATUS_PERIODE,
                innhold = ObjectMapper().createObjectNode(),
                gjelderReferanse = bpRef1,
                gjelderBarnReferanse = barnRef2,
            )
        dto.erGyldigForBarn(bmRef = bmRef1, bpRef = bpRef1, barnRef = barnRef1) shouldBe true
    }
}
