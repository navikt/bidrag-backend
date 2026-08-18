package no.nav.bidrag.aktoerregister.persistence.repository

import no.nav.bidrag.aktoerregister.persistence.entities.Hendelse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface HendelseRepository : JpaRepository<Hendelse, Int> {

    @Query(
        value = """
            SELECT sekvensnummer, aktoer_ident
            FROM  aktoerregister.hendelse h
            WHERE h.sekvensnummer >= :sekvensnummer
            ORDER BY sekvensnummer
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun hentAlleHendelserMedSekvensnummerOgIdent(sekvensnummer: Int, limit: Int): List<SekvensnummerOgIdent>
}

interface SekvensnummerOgIdent {
    val sekvensnummer: Int

    @Suppress("ktlint:standard:property-naming")
    val aktoer_ident: String
}
