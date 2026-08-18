package no.nav.bidrag.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.admin.persistence.entity.AktivForMiljø
import no.nav.bidrag.admin.persistence.entity.Endringslogg
import no.nav.bidrag.admin.persistence.entity.EndringsloggTilhørerSkjermbilde
import no.nav.bidrag.admin.persistence.entity.Endringstype
import no.nav.bidrag.commons.security.utils.TokenUtils
import java.time.LocalDate
import java.time.LocalDateTime

data class EndringsLoggDto(
    val id: Long,
    @Schema(description = "Dato når endringsloggen ble publisert")
    val dato: LocalDateTime,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val aktivFra: LocalDateTime?,
    val aktivTil: LocalDateTime?,
    @Schema(description = "Hvilken system/skjermbilde endringsloggen gjelder for")
    val gjelder: EndringsloggTilhørerSkjermbilde,
    val aktiveMiljøer: List<AktivForMiljø> = emptyList(),
    @Schema(description = "Tittel på endringsloggen")
    val tittel: String,
    val endringstyper: List<Endringstype>,
    @Schema(description = "Sammendrag av endringsloggen I HTML")
    val sammendrag: String,
    @Schema(
        description =
        "Om det er påkrevd å lese endringsloggen. " +
            "Dette skal føre til at det vises en modal første gang bruker åpner bidrag løsningen",
    )
    val erPåkrevd: Boolean,
    @Schema(
        description =
        "Om endringsloggen er lest av bruker. Dette vil være sann hvis bruker har lest alle endringene i endringsloggen",
    )
    val erLestAvBruker: Boolean,
    @Schema(
        description =
        "Om endringsloggen er sett av bruker. Dette vil være sann hvis bruker har sett endringsloggen men ikke har sett alle endringene i endringsloggen",
    )
    val erSettAvBruker: Boolean,
    @Schema(
        description =
        "Liste over endringer i endringsloggen. ",
    )
    val endringer: List<EndringsLoggEndringDto>,
)

data class EndringsLoggEndringDto(
    @Schema(description = "Innhold i endringen I HTML")
    val innhold: String,
    val tittel: String,
    val endringstype: Endringstype,
    val id: Long,
    val erLestAvBruker: Boolean,
)

fun Endringslogg.toDto(): EndringsLoggDto = EndringsLoggDto(
    id = id ?: -1,
    dato = aktivFraTidspunkt ?: opprettetTidspunkt,
    opprettetTidspunkt = opprettetTidspunkt,
    aktiv =
    aktivFraTidspunkt != null && aktivFraTidspunkt!! <= LocalDateTime.now() &&
        (aktivTilTidspunkt == null || aktivTilTidspunkt!! > LocalDateTime.now()),
    aktivFra = aktivFraTidspunkt,
    aktivTil = aktivTilTidspunkt,
    aktiveMiljøer = aktivForMiljø.toList(),
    tittel = tittel,
    sammendrag = sammendrag,
    gjelder = tilhørerSkjermbilde,
    erPåkrevd = erPåkrevd,
    endringstyper = endringer.map { it.endringstype },
    erLestAvBruker = erAlleLestAvBruker,
    endringer =
    endringer.sortedBy { it.rekkefølgeIndeks }.map {
        EndringsLoggEndringDto(
            id = it.id!!,
            innhold = it.innhold,
            tittel = it.tittel,
            endringstype = it.endringstype,
            erLestAvBruker =
            it.brukerLesinger.any {
                it.person.navIdent == TokenUtils.hentSaksbehandlerIdent() || TokenUtils.erApplikasjonsbruker()
            },
        )
    },
    erSettAvBruker =
    endringer.any { e ->
        e.brukerLesinger.any {
            it.person.navIdent == TokenUtils.hentSaksbehandlerIdent() || TokenUtils.erApplikasjonsbruker()
        }
    } ||
        brukerLesinger.any {
            it.person.navIdent == TokenUtils.hentSaksbehandlerIdent() || TokenUtils.erApplikasjonsbruker()
        },
)
