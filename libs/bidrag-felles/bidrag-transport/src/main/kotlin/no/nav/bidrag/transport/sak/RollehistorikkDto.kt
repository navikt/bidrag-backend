package no.nav.bidrag.transport.sak

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDateTime

data class RollehistorikkDto(
    @field:Schema(
        description = "Personens fødselsnummer. Påkrevd for alle",
    )
    @field:JsonProperty("fodselsnummer")
    val fødselsnummer: Personident? = null,
    @field:Schema(
        description =
            "Kode for rolletype tilsvarende kodene i T_KODE_ROLLETYPE. " +
                "Gyldige verdier er f.eks. BM (bidragsmottaker), BP (bidragspliktig) og BA (barn).",
    )
    val type: Rolletype,
    @field:Schema(
        description =
            "Reell mottaker (RM) for barnet. " +
                "Kan kun registreres for barn (BA), og kan representere både person eller samhandler (organisasjon/verge). " +
                "Dette feltet brukes i stedet for samhandlerIdent når bidrag skal utbetales til annen part enn barnets BM.",
    )
    val reellMottaker: ReellMottakerDto? = null,
    @field:Schema(
        description =
            "Beskrivelse av hva slags type endring som er gjort for rolle",
    )
    val typeEndring: TypeEndring? = null,
    @field:Schema(
        description =
            "Rollen er oppdatert av angitt saksbehandler eller applikasjon ",
    )
    val opprettetAv: String? = null,
    @field:Schema(
        description =
            "Tidspunkt rollen ble endret ",
    )
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)
