package no.nav.bidrag.transport.samhandler

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.transport.felles.AuditLogDto

data class SamhandlerDto(
    @param:Schema(description = "Identen til samhandler")
    val samhandlerId: SamhandlerId?,
    @param:Schema(description = "Navn på samhandler")
    val navn: String,
    @param:Schema(description = "Offentlig id for samhandlere.")
    val offentligId: String? = null,
    @param:Schema(
        description =
            "Type offentlig id. " +
                "F.eks ORG for norske organisasjonsnummere, " +
                "UTOR for utenlandske organisasjonsnummere, " +
                "FNR for norske personnummer.",
    )
    val offentligIdType: OffentligIdType? = null,
    @param:Schema(description = "Definerer hvilket område samhandleren er knyttet til.")
    val områdekode: Områdekode? = null,
    @param:Schema(description = "Språk til samhandleren.")
    val språk: Språk? = null,
    @param:Schema(description = "Samhandlerens adresse.")
    val adresse: AdresseDto? = null,
    @param:Schema(description = "Samhandlerens kontonummer.")
    val kontonummer: KontonummerDto? = null,
    @param:Schema(description = "Kontaktperson for samhandler. Benyttes primært av utland.")
    val kontaktperson: String? = null,
    @param:Schema(description = "Kontakt epost for samhandler. Benyttes primært av utland.")
    val kontaktEpost: String? = null,
    @param:Schema(description = "Kontakt telefon for samhandler. Benyttes primært av utland.")
    val kontaktTelefon: String? = null,
    @param:Schema(description = "Fritekstfelt for notater.")
    val notat: String? = null,
    @param:Schema(description = "Er samhandleren opphørt?")
    val erOpphørt: Boolean? = null,
    @param:Schema(description = "Liste over endringer på samhandleren.")
    val auditLog: List<AuditLogDto>? = null,
)
