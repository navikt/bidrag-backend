package no.nav.bidrag.domene.enums.beregning

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvslag
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.tilBisysResultatkode
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import org.junit.jupiter.api.Test

class ResultatkodeTest {
    @Test
    fun `Skal hente resultatkode basert på legacyKode`() {
        val resultatkode = Resultatkode.fraKode("A")
        val resultatkode2 = Resultatkode.fraKode("AVSLAG")
        val resultatkode3 = Resultatkode.fraKode("DIREKTE_OPPJØR")
        val resultatkode4 = Resultatkode.fraKode("GIBBERISH")

        resultatkode.shouldNotBeNull()
        resultatkode shouldBe Resultatkode.AVSLAG

        resultatkode2.shouldNotBeNull()
        resultatkode2 shouldBe Resultatkode.AVSLAG

        resultatkode3.shouldNotBeNull()
        resultatkode3 shouldBe Resultatkode.DIREKTE_OPPGJØR

        resultatkode4.shouldBeNull()
    }

    @Test
    fun `Skal hente avslagskoder`() {
        val resultatkoder = Resultatkode.alleMedType(Resultatkode.ResultatkodeType.AVSLAG)

        resultatkoder.shouldHaveSize(36)
    }

    @Test
    fun `Skal returnere bisys resultatkode basert på vedtakstype`() {
        Resultatkode.IKKE_OMSORG.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("AIO")
        Resultatkode.IKKE_OMSORG.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OIO")

        Resultatkode.AVSLAG_PRIVAT_AVTALE_BIDRAG.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("APA")
        Resultatkode.AVSLAG_PRIVAT_AVTALE_BIDRAG.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OPA")

        Resultatkode.IKKE_INNKREVING_AV_BIDRAG.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("AIB")
        Resultatkode.IKKE_INNKREVING_AV_BIDRAG.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OIB")

        Resultatkode.AVSLAG_OVER_18_ÅR.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("A18")
        Resultatkode.AVSLAG_OVER_18_ÅR.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OH2")

        Resultatkode.AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("ARA")
        Resultatkode.AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("ORA")

        Resultatkode.IKKE_OMSORG.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("AIO")
        Resultatkode.IKKE_OMSORG.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OIO")

        Resultatkode.AVSLAG_HØY_INNTEKT.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("AHI")
        Resultatkode.AVSLAG_HØY_INNTEKT.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("OHI")

        Resultatkode.FORHØYET_FORSKUDD_100_PROSENT.tilBisysResultatkode(Vedtakstype.FASTSETTELSE).shouldBe("100")
        Resultatkode.FORHØYET_FORSKUDD_11_ÅR_125_PROSENT.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("125")
        Resultatkode.REDUSERT_FORSKUDD_50_PROSENT.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("50")
        Resultatkode.ORDINÆRT_FORSKUDD_75_PROSENT.tilBisysResultatkode(Vedtakstype.OPPHØR).shouldBe("75")
    }

    @Test
    fun `Skal hente avslagskoder og opphørskoder`() {
        Resultatkode.PRIVAT_AVTALE.erAvslag().shouldBeFalse()
        Resultatkode.AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG.erAvslag().shouldBeTrue()
        Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE.erAvslag().shouldBeTrue()
        Resultatkode.BARNET_ER_SELVFORSØRGET.erAvslag().shouldBeTrue()
    }
}
