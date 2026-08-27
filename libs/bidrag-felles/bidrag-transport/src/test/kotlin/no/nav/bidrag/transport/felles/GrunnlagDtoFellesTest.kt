package no.nav.bidrag.transport.felles

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnlagDtoFellesTest {
    @Test
    fun `Skal opprette grunnlag`() {
        val grunnlag =
            GrunnlagDto(
                "Person-150-BM",
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                POJONode(Person(Personident("123213213213"), "Navn Navnesen", LocalDate.parse("2020-01-01"))),
            )

        grunnlag.toString() shouldBe
            "PERSON_BIDRAGSMOTTAKER - referanse=Person-150-BM, gjelderReferanse=null, grunnlagsreferanseListe=<tomt>, innhold={\"ident\":\"123213213213\",\"navn\":\"Navn Navnesen\",\"fødselsdato\":\"2020-01-01\",\"bidragsmottaker\":null,\"delAvOpprinneligBehandling\":true,\"stønadstype\":null}"
    }

    @Test
    fun `Skal ikke kunne legge til to grunnlagDto med samme referanse`() {
        val referanse = "Person_Bidragsmottaker_20040101"

        val grunnlag1 =
            GrunnlagDto(
                referanse,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                POJONode(Person(Personident("123213213213"), "Navn Navnesen", LocalDate.parse("2020-01-01"))),
                grunnlagsreferanseListe = listOf("1", "2"),
                gjelderReferanse = "test",
            )
        val grunnlag1Duplicate =
            GrunnlagDto(
                referanse,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                POJONode(Person(Personident("123213213213"), "Navn Navnesen", LocalDate.parse("2020-01-01"))),
                grunnlagsreferanseListe = listOf("2", "1"),
                gjelderReferanse = "test",
            )
        val grunnlag2 =
            GrunnlagDto(
                referanse,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                POJONode(Person(Personident("123213213213"), "Navn Navnesen", LocalDate.parse("2020-01-01"))),
                grunnlagsreferanseListe = listOf("1", "2"),
                gjelderReferanse = "test2",
            )
        val grunnlag3 =
            GrunnlagDto(
                referanse,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                POJONode(Person(Personident("123213213213"), "Navn Navnesen2", LocalDate.parse("2020-01-01"))),
                grunnlagsreferanseListe = listOf("1", "2"),
                gjelderReferanse = "test2",
            )

        val grunnlagSet = setOf(grunnlag1, grunnlag1Duplicate, grunnlag2, grunnlag3)
        grunnlagSet shouldHaveSize 2
        grunnlag1 shouldBeEqual grunnlag1Duplicate
        grunnlag1 shouldBeEqual grunnlag2
        grunnlag1 shouldNotBeEqual grunnlag3
        grunnlag2 shouldNotBeEqual grunnlag3
    }
}
