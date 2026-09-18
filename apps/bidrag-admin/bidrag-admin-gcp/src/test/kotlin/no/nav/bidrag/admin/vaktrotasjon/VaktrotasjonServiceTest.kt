package no.nav.bidrag.admin.vaktrotasjon

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.slack_lists.SlackListsItemsListRequest
import com.slack.api.methods.request.slack_lists.SlackListsItemsUpdateRequest
import com.slack.api.methods.response.slack_lists.SlackListsItemsListResponse
import com.slack.api.methods.response.slack_lists.SlackListsItemsUpdateResponse
import com.slack.api.model.list.ListRecord
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.commons.service.slack.SlackService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class VaktrotasjonServiceTest {

    @MockK(relaxed = true)
    private lateinit var slackService: SlackService

    private lateinit var vaktrotasjonService: VaktrotasjonService

    private val methodsClient: MethodsClient = mockk()

    @BeforeEach
    fun setup() {
        vaktrotasjonService = VaktrotasjonService(slackService, "F0C1R4W86QK")
        every { slackService.client } returns methodsClient
        every { methodsClient.slackListsItemsUpdate(any<SlackListsItemsUpdateRequest>()) } returns
            SlackListsItemsUpdateResponse().apply { isOk = true }
    }

    @Test
    fun `kjørRotasjon velger rad med eldst Sist vaktdato og roterer til den personen`() {
        val rader =
            listOf(
                byggRad(radId = "1", slackUserId = "U_NYLIG", sistVaktdato = LocalDate.now().minusWeeks(1)),
                byggRad(radId = "2", slackUserId = "U_ELDST", sistVaktdato = LocalDate.now().minusWeeks(8)),
                byggRad(radId = "3", slackUserId = "U_MIDT", sistVaktdato = LocalDate.now().minusWeeks(4)),
            )
        every { methodsClient.slackListsItemsList(any<SlackListsItemsListRequest>()) } returns
            SlackListsItemsListResponse().apply {
                isOk = true
                items = rader
            }

        vaktrotasjonService.kjørRotasjon()

        verify { slackService.sendMelding(match { it.contains("<@U_ELDST>") }) }
    }

    @Test
    fun `kjørRotasjon prioriterer rad uten Sist vaktdato satt`() {
        val rader =
            listOf(
                byggRad(radId = "1", slackUserId = "U_MED_DATO", sistVaktdato = LocalDate.now().minusYears(1)),
                byggRad(radId = "2", slackUserId = "U_UTEN_DATO", sistVaktdato = null),
            )
        every { methodsClient.slackListsItemsList(any<SlackListsItemsListRequest>()) } returns
            SlackListsItemsListResponse().apply {
                isOk = true
                items = rader
            }

        vaktrotasjonService.kjørRotasjon()

        verify { slackService.sendMelding(match { it.contains("<@U_UTEN_DATO>") }) }
    }

    @Test
    fun `kjørRotasjon oppdaterer Sist vaktdato til dagens dato på riktig rad`() {
        val rad = byggRad(radId = "42", slackUserId = "U_ELDST", sistVaktdato = LocalDate.now().minusWeeks(8))
        every { methodsClient.slackListsItemsList(any<SlackListsItemsListRequest>()) } returns
            SlackListsItemsListResponse().apply {
                isOk = true
                items = listOf(rad)
            }
        val requestSlot = slot<SlackListsItemsUpdateRequest>()
        every { methodsClient.slackListsItemsUpdate(capture(requestSlot)) } returns
            SlackListsItemsUpdateResponse().apply { isOk = true }

        vaktrotasjonService.kjørRotasjon()

        val cell = requestSlot.captured.cells.single()
        cell.rowId shouldBe "42"
        cell.date shouldBe listOf(LocalDate.now().toString())
    }

    @Test
    fun `byggVaktmelding inneholder korrekt Slack-mention av vakthavende`() {
        val melding = vaktrotasjonService.byggVaktmelding("U12345")

        melding.contains("Giv akt! Du har vakt, <@U12345>!") shouldBe true
    }

    private fun byggRad(
        radId: String,
        slackUserId: String,
        sistVaktdato: LocalDate?,
    ): ListRecord {
        val vakthavendeFelt =
            ListRecord.Field
                .builder()
                .key(VaktrotasjonService.KOLONNE_VAKTHAVENDE)
                .columnId("col-vakthavende")
                .user(listOf(slackUserId))
                .build()
        val sistVaktdatoFelt =
            ListRecord.Field
                .builder()
                .key(VaktrotasjonService.KOLONNE_SIST_VAKTDATO)
                .columnId("col-sist-vaktdato")
                .also { builder -> sistVaktdato?.let { builder.date(listOf(it.toString())) } }
                .build()

        return ListRecord
            .builder()
            .id(radId)
            .fields(listOf(vakthavendeFelt, sistVaktdatoFelt))
            .build()
    }
}
