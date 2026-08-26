package no.nav.bidrag.transport.felles

import io.kotest.matchers.shouldBe
import no.nav.bidrag.transport.samhandler.SøkSamhandlerQuery
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class QueryObjectKtTest {
    @Test
    fun `toQueryParams mapper alle verdier til MultivalueMap korrekt for et objekt`() {
        val søkSamhandlerQuery =
            TestQueryObject(
                int = 1,
                string = "string",
                boolean = true,
                date = LocalDate.of(2012, 5, 17),
                dateTime = LocalDateTime.of(2012, 5, 17, 8, 0, 0),
                list = listOf("string1", "string2", "string3"),
            )

        val multiValueMap = søkSamhandlerQuery.toQueryParams()

        multiValueMap["int"] shouldBe listOf("1")
        multiValueMap["string"] shouldBe listOf("string")
        multiValueMap["boolean"] shouldBe listOf("true")
        multiValueMap["date"] shouldBe listOf("2012-05-17")
        multiValueMap["dateTime"] shouldBe listOf("2012-05-17T08:00:00")
        multiValueMap["list"] shouldBe listOf("string1", "string2", "string3")
    }

    @Test
    fun `toQueryParams ignorerer null-verdier`() {
        val søkSamhandlerQuery =
            TestQueryObject(
                int = null,
                string = null,
                boolean = null,
                date = null,
                dateTime = null,
                list = null,
            )

        val multiValueMap = søkSamhandlerQuery.toQueryParams()

        multiValueMap.containsKey("int") shouldBe false
        multiValueMap.containsKey("string") shouldBe false
        multiValueMap.containsKey("boolean") shouldBe false
        multiValueMap.containsKey("date") shouldBe false
        multiValueMap.containsKey("dateTime") shouldBe false
        multiValueMap.containsKey("list") shouldBe false
    }

    @Test
    fun `toQueryParams ignorerer tomme lister`() {
        val søkSamhandlerQuery =
            TestQueryObject(
                int = null,
                string = null,
                boolean = null,
                date = null,
                dateTime = null,
                list = listOf(),
            )

        val multiValueMap = søkSamhandlerQuery.toQueryParams()

        multiValueMap.containsKey("list") shouldBe false
    }

    @Test
    fun `toQueryParams bruker JsonProperty`() {
        val søkSamhandlerQuery =
            SøkSamhandlerQuery(
                navn = "navn",
                postnummer = "postnummer",
                område = "område",
            )

        val multiValueMap = søkSamhandlerQuery.toQueryParams()

        multiValueMap.containsKey("omrade") shouldBe true
        multiValueMap.containsKey("område") shouldBe false
    }

    inner class TestQueryObject(
        val int: Int?,
        val string: String?,
        val boolean: Boolean?,
        val date: LocalDate?,
        val dateTime: LocalDateTime?,
        val list: List<String>?,
    ) : QueryObject
}
