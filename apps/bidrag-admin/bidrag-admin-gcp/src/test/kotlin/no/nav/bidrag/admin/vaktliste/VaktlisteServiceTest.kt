package no.nav.bidrag.admin.vaktliste

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.files.FilesInfoRequest
import com.slack.api.methods.request.slack_lists.SlackListsItemsListRequest
import com.slack.api.methods.request.slack_lists.SlackListsItemsUpdateRequest
import com.slack.api.methods.response.files.FilesInfoResponse
import com.slack.api.methods.response.slack_lists.SlackListsItemsListResponse
import com.slack.api.methods.response.slack_lists.SlackListsItemsUpdateResponse
import com.slack.api.model.File
import com.slack.api.model.list.ListColumn
import com.slack.api.model.list.ListMetadata
import com.slack.api.model.list.ListRecord
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.admin.service.VaktlisteService
import no.nav.bidrag.commons.service.slack.SlackMelding
import no.nav.bidrag.commons.service.slack.SlackService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class VaktlisteServiceTest {

    @MockK(relaxed = true)
    private lateinit var slackService: SlackService

    private lateinit var vaktlisteService: VaktlisteService

    private val methodsClient: MethodsClient = mockk()

    @BeforeEach
    fun setup() {
        vaktlisteService = VaktlisteService(slackService, "F0C1R4W86QK")
        every { slackService.client } returns methodsClient
        every { slackService.sendMelding(any()) } returns
            SlackMelding(slackService = slackService, ts = "1234.5678", threadTs = null, channel = "C123")
        every { methodsClient.slackListsItemsUpdate(any<SlackListsItemsUpdateRequest>()) } returns
            SlackListsItemsUpdateResponse().apply { isOk = true }
        // Slack genererer interne feltnøkler (f.eks. "Col0C2L3X5VNE") for kolonner opprettet i UI-et,
        // som ikke samsvarer med visningsnavnet. Skjemaet må derfor slås opp for å finne riktig nøkkel.
        every { methodsClient.filesInfo(any<FilesInfoRequest>()) } returns
            FilesInfoResponse().apply {
                isOk = true
                file =
                    File
                        .builder()
                        .listMetadata(
                            ListMetadata
                                .builder()
                                .schema(
                                    listOf(
                                        ListColumn
                                            .builder()
                                            .name(VaktlisteService.KOLONNE_VAKTHAVENDE)
                                            .id(VAKTHAVENDE_COLUMN_ID)
                                            .key(VAKTHAVENDE_KOLONNENØKKEL)
                                            .build(),
                                        ListColumn
                                            .builder()
                                            .name(VaktlisteService.KOLONNE_SIST_VAKTDATO)
                                            .id(SIST_VAKTDATO_COLUMN_ID)
                                            .key(SIST_VAKTDATO_KOLONNENØKKEL)
                                            .build(),
                                        ListColumn
                                            .builder()
                                            .name(VaktlisteService.KOLONNE_UKENS_VAKTHAVENDE)
                                            .id(UKENS_COLUMN_ID)
                                            .key(UKENS_KOLONNENØKKEL)
                                            .build(),
                                    ),
                                ).build(),
                        ).build()
            }
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

        vaktlisteService.roterVakthavende()

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

        vaktlisteService.roterVakthavende()

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

        vaktlisteService.roterVakthavende()

        val cell = requestSlot.captured.cells.single { it.date != null }
        cell.rowId shouldBe "42"
        cell.date shouldBe listOf(LocalDate.now().toString())
    }

    @Test
    fun `byggVaktmelding inneholder korrekt Slack-mention av vakthavende`() {
        val melding = VaktlisteService.byggVaktmelding("U12345")

        melding.contains("Giv akt! Du har vakt, <@U12345>!") shouldBe true
    }

    @Test
    fun `kjørRotasjon huker av Ukens for ny vakthavende og fjerner huken fra forrige vakthavende`() {
        val forrigeVakthavende =
            byggRad(radId = "1", slackUserId = "U_FORRIGE", sistVaktdato = LocalDate.now().minusWeeks(1), erUkensVakthavende = true)
        val nesteVakthavende =
            byggRad(radId = "2", slackUserId = "U_NESTE", sistVaktdato = LocalDate.now().minusWeeks(8), erUkensVakthavende = false)
        every { methodsClient.slackListsItemsList(any<SlackListsItemsListRequest>()) } returns
            SlackListsItemsListResponse().apply {
                isOk = true
                items = listOf(forrigeVakthavende, nesteVakthavende)
            }
        val requestSlot = slot<SlackListsItemsUpdateRequest>()
        every { methodsClient.slackListsItemsUpdate(capture(requestSlot)) } returns
            SlackListsItemsUpdateResponse().apply { isOk = true }

        vaktlisteService.roterVakthavende()

        val cells = requestSlot.captured.cells
        cells.count { it.columnId == UKENS_COLUMN_ID && it.rowId == "2" && it.checkbox == true } shouldBe 1
        cells.count { it.columnId == UKENS_COLUMN_ID && it.rowId == "1" && it.checkbox == false } shouldBe 1
    }

    @Test
    fun `roterVakthavende logger visningsnavnet fremfor slackId når navn er tilgjengelig`() {
        val rad = byggRad(radId = "7", slackUserId = "U_NAVNGITT", sistVaktdato = LocalDate.now().minusWeeks(3), navn = "Kari Nordmann")
        every { methodsClient.slackListsItemsList(any<SlackListsItemsListRequest>()) } returns
            SlackListsItemsListResponse().apply {
                isOk = true
                items = listOf(rad)
            }

        val rotLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        rotLogger.addAppender(appender)

        try {
            vaktlisteService.roterVakthavende()
        } finally {
            rotLogger.detachAppender(appender)
        }

        val meldinger = appender.list.map { it.formattedMessage }
        meldinger.any { it.contains("Kari Nordmann") } shouldBe true
        meldinger.none { it.contains("U_NAVNGITT") } shouldBe true
    }

    private fun byggRad(
        radId: String,
        slackUserId: String,
        sistVaktdato: LocalDate?,
        erUkensVakthavende: Boolean = false,
        navn: String? = null,
    ): ListRecord {
        val vakthavendeFelt =
            ListRecord.Field
                .builder()
                .key(VAKTHAVENDE_KOLONNENØKKEL)
                .columnId("col-vakthavende")
                .user(listOf(slackUserId))
                .build()
        val sistVaktdatoFelt =
            ListRecord.Field
                .builder()
                .key(SIST_VAKTDATO_KOLONNENØKKEL)
                .columnId("col-sist-vaktdato")
                .also { builder -> sistVaktdato?.let { builder.date(listOf(it.toString())) } }
                .build()
        val ukensFelt =
            ListRecord.Field
                .builder()
                .key(UKENS_KOLONNENØKKEL)
                .columnId(UKENS_COLUMN_ID)
                .checkbox(erUkensVakthavende)
                .build()
        val felter =
            buildList {
                add(vakthavendeFelt)
                add(sistVaktdatoFelt)
                add(ukensFelt)
                navn?.let {
                    add(
                        ListRecord.Field
                            .builder()
                            .key(VaktlisteService.KOLONNE_NAVN)
                            .columnId("col-navn")
                            .value(it)
                            .build(),
                    )
                }
            }

        return ListRecord
            .builder()
            .id(radId)
            .fields(felter)
            .build()
    }

    companion object {
        // Eksempler på interne, obfuskerte feltnøkler slik Slack faktisk genererer dem for kolonner.
        private const val VAKTHAVENDE_KOLONNENØKKEL = "Col0C2L3X5VNE"
        private const val SIST_VAKTDATO_KOLONNENØKKEL = "Col0C2PGC01J9"
        private const val UKENS_KOLONNENØKKEL = "Col0C2UKENSXX"
        private const val VAKTHAVENDE_COLUMN_ID = "Col0C2L3X5VNE"
        private const val SIST_VAKTDATO_COLUMN_ID = "Col0C2PGC01J9"
        private const val UKENS_COLUMN_ID = "Col0C2UKENSXX"
    }
}
