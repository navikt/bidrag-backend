package no.nav.bidrag.admin.service

import com.slack.api.methods.request.slack_lists.SlackListsItemsListRequest
import com.slack.api.methods.request.slack_lists.SlackListsItemsUpdateRequest
import com.slack.api.model.list.ListRecord
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger { }

/**
 * Henter og roterer ukens vakthavende basert på "Bidrag utviklere"-listen i Slack.
 *
 * Personen som har det lengst siden "Sist vaktdato" (eller aldri har hatt vakt) plukkes som neste
 * vakthavende. Etter at vaktmeldingen er sendt, oppdateres "Sist vaktdato" på raden til dagens dato.
 */
@Service
class VaktrotasjonService(
    private val slackService: SlackService,
    @param:Value($$"${SLACK_VAKTLISTE_ID}") private val listeId: String,
) {
    companion object {
        internal const val KOLONNE_VAKTHAVENDE = "Vakthavende"
        internal const val KOLONNE_SIST_VAKTDATO = "Sist vaktdato"
    }

    fun kjørRotasjon() {
        val rader = hentVaktrader()
        if (rader.isEmpty()) {
            LOGGER.warn { "Fant ingen rader i vaktlisten $listeId. Avbryter rotasjon." }
            return
        }

        val nesteVakthavende = rader.minByOrNull { it.sistVaktdato ?: LocalDate.MIN } ?: return
        val slackUserId = nesteVakthavende.slackUserId
        if (slackUserId == null) {
            LOGGER.error {
                "Fant rad ${nesteVakthavende.radId} uten gyldig verdi i kolonnen \"$KOLONNE_VAKTHAVENDE\". " +
                    "Tilgjengelige feltnøkler på raden: ${nesteVakthavende.tilgjengeligeFeltnøkler}"
            }
            return
        }

        slackService.sendMelding(byggVaktmelding(slackUserId))
        oppdaterSistVaktdato(nesteVakthavende)
        LOGGER.info { "Vakt rotert til <@$slackUserId> (rad ${nesteVakthavende.radId})." }
    }

    private fun hentVaktrader(): List<VaktRad> {
        val respons =
            slackService.client.slackListsItemsList(
                SlackListsItemsListRequest
                    .builder()
                    .listId(listeId)
                    .build(),
            )

        if (!respons.isOk) {
            LOGGER.error { "Feil ved henting av vaktlisten $listeId: ${respons.error}" }
            return emptyList()
        }

        return respons.items.map { it.tilVaktRad() }
    }

    private fun oppdaterSistVaktdato(rad: VaktRad) {
        val columnId = rad.sistVaktdatoColumnId
        if (columnId == null) {
            LOGGER.error { "Fant ikke kolonne-id for \"$KOLONNE_SIST_VAKTDATO\" på rad ${rad.radId}. Klarte ikke å oppdatere dato." }
            return
        }

        val cellOppdatering =
            ListRecord.CellUpdate
                .builder()
                .rowId(rad.radId)
                .columnId(columnId)
                .date(listOf(LocalDate.now().toString()))
                .build()

        val respons =
            slackService.client.slackListsItemsUpdate(
                SlackListsItemsUpdateRequest
                    .builder()
                    .listId(listeId)
                    .cells(listOf(cellOppdatering))
                    .build(),
            )

        if (!respons.isOk) {
            LOGGER.error { "Feil ved oppdatering av \"$KOLONNE_SIST_VAKTDATO\" på rad ${rad.radId}: ${respons.error}" }
        }
    }

    private fun ListRecord.tilVaktRad(): VaktRad {
        val vakthavendeFelt = fields.finnFelt(KOLONNE_VAKTHAVENDE)
        val sistVaktdatoFelt = fields.finnFelt(KOLONNE_SIST_VAKTDATO)

        return VaktRad(
            radId = id,
            slackUserId = vakthavendeFelt?.user?.firstOrNull() ?: vakthavendeFelt?.value ?: vakthavendeFelt?.text,
            sistVaktdato = sistVaktdatoFelt?.date?.firstOrNull()?.let(::parseDato),
            sistVaktdatoColumnId = sistVaktdatoFelt?.columnId,
            tilgjengeligeFeltnøkler = fields.mapNotNull { it.key },
        )
    }

    private fun List<ListRecord.Field>.finnFelt(kolonnenavn: String) = firstOrNull { it.key?.equals(kolonnenavn, ignoreCase = true) == true }

    private fun parseDato(verdi: String): LocalDate? = try {
        LocalDate.parse(verdi)
    } catch (e: Exception) {
        LOGGER.warn(e) { "Klarte ikke å tolke dato \"$verdi\" i kolonnen \"$KOLONNE_SIST_VAKTDATO\"." }
        null
    }

    internal fun byggVaktmelding(slackUserId: String) = """
        Giv akt! Du har vakt, <@$slackUserId>!

        Dette innebærer at du skal gjøre følgende:

        - Følg med på om det er kritiske sårbarheter som må fikses. Alt som har over CVSS score på 9 har vi 24 timer på å fikse.
               - https://console.nav.cloud.nais.io/team/bidrag
               - https://console.nav.cloud.nais.io/team/farskapsportal
        - En fin oversikt over hva som bør prioriteres finnes også her: https://tpt.ansatt.nav.no/nb/prioritization
        - Se over dependabot pr'er.
        - Se over codescanning i repoene om noe må fikses eller følges opp.
        - Følg med i #team-bidrag-monitorering og #team-bidrag-varsel. Marker alle saker som du har sett på slik at vi vet de er fulgt opp.

        Du skal ikke løse alt på egen hånd, fordel oppgaver til de aktuelle i teamet.

        Har du ikke mulighet denne uken? Ping i denne tråden og be noen ta over for deg!
    """.trimIndent()
}

internal data class VaktRad(
    val radId: String,
    val slackUserId: String?,
    val sistVaktdato: LocalDate?,
    val sistVaktdatoColumnId: String?,
    val tilgjengeligeFeltnøkler: List<String>,
)
