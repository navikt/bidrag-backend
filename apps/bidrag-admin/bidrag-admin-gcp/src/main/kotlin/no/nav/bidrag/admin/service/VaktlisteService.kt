package no.nav.bidrag.admin.service

import com.slack.api.methods.request.files.FilesInfoRequest
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
 * Personen som har det lengst siden "Sist vaktdato" plukkes som neste
 * vakthavende. Etter at vaktmeldingen er sendt, oppdateres "Sist vaktdato" på raden til dagens dato,
 * "Ukens"-huken settes på den nye vakthavendes rad, og fjernes fra alle andre rader som har den satt.
 */
@Service
class VaktlisteService(
    private val slackService: SlackService,
    @param:Value($$"${SLACK_VAKTLISTE_ID}") private val vaktlisteId: String,
) {
    companion object {
        internal const val KOLONNE_VAKTHAVENDE = "Vakthavende"
        internal const val KOLONNE_SIST_VAKTDATO = "Sist vaktdato"
        internal const val KOLONNE_UKENS_VAKTHAVENDE = "Ukens"
        internal const val KOLONNE_NAVN = "name"

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

    fun roterVakthavende() {
        val (rader, kolonnenøkler) = hentRaderFraListen()
        if (rader.isEmpty()) {
            throw VaktlisteException("Fant ingen rader i vaktlisten $vaktlisteId. Avbryter rotasjon.")
        }

        val nesteVakthavende = rader.minByOrNull { it.sistVaktdato ?: LocalDate.MIN }
            ?: throw VaktlisteException("Fant ingen rad å velge vakthavende fra i vaktlisten $vaktlisteId.")
        val slackUserId = nesteVakthavende.slackBrukerId ?: throw VaktlisteException(
            "Fant rad ${nesteVakthavende.id} uten gyldig verdi i kolonnen \"$KOLONNE_VAKTHAVENDE\". " +
                "Tilgjengelige feltnøkler på raden: ${nesteVakthavende.tilgjengeligeNøkler}",
        )

        val melding = slackService.sendMelding(byggVaktmelding(slackUserId))
        if (!melding.vellykket) {
            throw VaktlisteException("Feil ved sending av vaktmelding til Slack: ${melding.feil}")
        }
        oppdaterVaktstatus(rader, nesteVakthavende, kolonnenøkler)

        val visningsnavn = nesteVakthavende.navn?.takeIf { it.isNotBlank() } ?: slackUserId
        LOGGER.info { "Vakt rotert til $visningsnavn." }
    }

    private fun hentRaderFraListen(): Pair<List<VaktlisteRad>, Map<String, KolonneSkjema>> {
        val kolonnenøkler = hentKolonnenøkler()

        val respons =
            slackService.client.slackListsItemsList(
                SlackListsItemsListRequest
                    .builder()
                    .listId(vaktlisteId)
                    .build(),
            )

        if (!respons.isOk) {
            throw VaktlisteException("Feil ved henting av vaktlisten $vaktlisteId: ${respons.error}")
        }

        return respons.items.map { it.tilVaktRad(kolonnenøkler) } to kolonnenøkler
    }

    /**
     * Slack Lists gir kolonner en internt generert feltnøkkel (f.eks. "Col0C2L3X5VNE") som ikke
     * nødvendigvis samsvarer med kolonnens visningsnavn. Vi må derfor slå opp listens skjema for å
     * finne riktig feltnøkkel og kolonne-id for kolonnene "Vakthavende", "Sist vaktdato" og "Ukens"
     * før vi kan lese og oppdatere radene.
     */
    private fun hentKolonnenøkler(): Map<String, KolonneSkjema> {
        val respons =
            slackService.client.filesInfo(
                FilesInfoRequest
                    .builder()
                    .file(vaktlisteId)
                    .build(),
            )

        if (!respons.isOk) {
            throw VaktlisteException("Feil ved henting av skjema for vaktlisten $vaktlisteId: ${respons.error}")
        }

        val skjema = respons.file?.listMetadata?.schema
            ?: throw VaktlisteException("Fant ikke skjema for vaktlisten $vaktlisteId.")

        return skjema
            .mapNotNull { kolonne -> kolonne.name?.let { navn -> navn to KolonneSkjema(nøkkel = kolonne.key ?: kolonne.id, id = kolonne.id) } }
            .toMap()
    }

    /**
     * Oppdaterer "Sist vaktdato" og "Ukens"-huken til ny vakthavende, og fjerner huken på "Ukens"
     * fra alle andre rader som eventuelt har den satt fra tidligere.
     */
    private fun oppdaterVaktstatus(
        rader: List<VaktlisteRad>,
        nesteVakthavende: VaktlisteRad,
        kolonnenøkler: Map<String, KolonneSkjema>,
    ) {
        val sistVaktdatoId = nesteVakthavende.sistVaktdatoId
            ?: throw VaktlisteException(
                "Fant ikke kolonne-id for \"$KOLONNE_SIST_VAKTDATO\" på rad ${nesteVakthavende.id}. Klarte ikke å oppdatere dato.",
            )
        val ukensId = kolonnenøkler[KOLONNE_UKENS_VAKTHAVENDE]?.id
            ?: throw VaktlisteException("Fant ikke kolonne-id for \"$KOLONNE_UKENS_VAKTHAVENDE\" i vaktlisten $vaktlisteId.")

        val oppdateringer =
            buildList {
                // Oppdaterer siste vakt dato
                add(
                    ListRecord.CellUpdate
                        .builder()
                        .rowId(nesteVakthavende.id)
                        .columnId(sistVaktdatoId)
                        .date(listOf(LocalDate.now().toString()))
                        .build(),
                )
                // Setter til ukens vakthavende
                add(
                    ListRecord.CellUpdate
                        .builder()
                        .rowId(nesteVakthavende.id)
                        .columnId(ukensId)
                        .checkbox(true)
                        .build(),
                )
                // Fjerner andre som er satt som ukens vakthavende
                rader
                    .filter { it.id != nesteVakthavende.id && it.erUkensVakthavende }
                    .forEach { rad ->
                        add(
                            ListRecord.CellUpdate
                                .builder()
                                .rowId(rad.id)
                                .columnId(ukensId)
                                .checkbox(false)
                                .build(),
                        )
                    }
            }

        val respons =
            slackService.client.slackListsItemsUpdate(
                SlackListsItemsUpdateRequest
                    .builder()
                    .listId(vaktlisteId)
                    .cells(oppdateringer)
                    .build(),
            )

        if (!respons.isOk) {
            throw VaktlisteException("Feil ved oppdatering av vaktstatus i vaktlisten $vaktlisteId: ${respons.error}")
        }
    }

    private fun ListRecord.tilVaktRad(kolonnenøkler: Map<String, KolonneSkjema>): VaktlisteRad {
        val vakthavendeFelt = fields.finnFelt(kolonnenøkler, KOLONNE_VAKTHAVENDE)
        val sistVaktdatoFelt = fields.finnFelt(kolonnenøkler, KOLONNE_SIST_VAKTDATO)
        val ukensFelt = fields.finnFelt(kolonnenøkler, KOLONNE_UKENS_VAKTHAVENDE)
        val navnFelt = fields.finnFelt(kolonnenøkler, KOLONNE_NAVN)

        return VaktlisteRad(
            id = id,
            slackBrukerId = vakthavendeFelt?.user?.firstOrNull() ?: vakthavendeFelt?.value ?: vakthavendeFelt?.text,
            navn = navnFelt?.value ?: navnFelt?.text,
            sistVaktdato = sistVaktdatoFelt?.date?.firstOrNull()?.let(::parseDato),
            sistVaktdatoId = sistVaktdatoFelt?.columnId,
            erUkensVakthavende = ukensFelt?.checkbox ?: false,
            tilgjengeligeNøkler = fields.mapNotNull { it.key },
        )
    }

    private fun List<ListRecord.Field>.finnFelt(
        kolonnenøkler: Map<String, KolonneSkjema>,
        kolonnenavn: String,
    ): ListRecord.Field? {
        val nøkkel = kolonnenøkler[kolonnenavn]?.nøkkel ?: kolonnenavn
        return firstOrNull { it.key?.equals(nøkkel, ignoreCase = true) == true }
    }

    private fun parseDato(verdi: String): LocalDate? = try {
        LocalDate.parse(verdi)
    } catch (e: Exception) {
        LOGGER.warn(e) { "Klarte ikke å tolke dato \"$verdi\" i kolonnen \"$KOLONNE_SIST_VAKTDATO\"." }
        null
    }
}

internal data class VaktlisteRad(
    val id: String,
    val slackBrukerId: String?,
    val navn: String?,
    val sistVaktdato: LocalDate?,
    val sistVaktdatoId: String?,
    val erUkensVakthavende: Boolean,
    val tilgjengeligeNøkler: List<String>,
)

private data class KolonneSkjema(val nøkkel: String, val id: String)

class VaktlisteException(message: String) : RuntimeException(message)
