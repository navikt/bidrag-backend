package no.nav.bidrag.sak.repository

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringTestRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.LocalDateTime

class VedtakOverføringRepositoryIT : SpringTestRunner() {
    @Autowired
    private lateinit var vedtakOverføringRepository: VedtakOverføringRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `countVedtakGrunnlagOverførtForSak teller rad med vedtakId og grunnlag satt`() {
        val saksnummer = Saksnummer("555001")
        val soknadId = 42

        // Én rad som matcher saksnr + soknadId og har begge kolonner satt.
        // (sokn_id og rolle_hist_id har unike indekser, så vi kan kun ha én
        //  rad per soknadId — derfor holder det med denne ene raden.)
        jdbcTemplate.update(
            """
            INSERT INTO T_VEDTAK_OVERFORING
                (saksnr, sokn_id, rolle_hist_id, vedtak_id_bidrag_vedtak, vedtak_overfort_med_grunnlag,
                 vedtak_timestamp_bisys, status, opprettet_timestamp)
            VALUES (?, ?, ?, ?, ?, CURRENT TIMESTAMP, 'OVERFORT', CURRENT TIMESTAMP)
            """,
            saksnummer.verdi,
            soknadId,
            1001,
            99,
            Timestamp.valueOf(LocalDateTime.now()),
        )

        val antall = vedtakOverføringRepository.countVedtakGrunnlagOverførtForSak(saksnummer, soknadId)

        // Bakgrunn: i produksjon (DB2 for z/OS) kaster `!= null` (→ SQL `<> null`)
        // SQLCODE=-206 fordi z/OS-parseren tolker `null` som et kolonnenavn.
        //
        // DB2 LUW (testcontaineren) er mildere: den aksepterer `<> null`, evaluerer
        // til UNKNOWN og returnerer 0 rader uten exception. Vi kan derfor ikke
        // reprodusere selve -206-feilen her, men bugget fanges likevel:
        //   - gammel kode (`!= null` → `<> null`): returnerer 0 → testen feiler
        //   - riktig kode (`is not null`):          returnerer 1 → testen er grønn
        antall shouldBe 1
    }

    @Test
    fun `finnVedtakIdBidragVedtakForSak returnerer vedtakId fra ny løsning`() {
        val saksnummer = Saksnummer("555002")
        val soknadId = 77

        jdbcTemplate.update(
            """
            INSERT INTO T_VEDTAK_OVERFORING
                (saksnr, sokn_id, rolle_hist_id, vedtak_id_bidrag_vedtak,
                 vedtak_timestamp_bisys, status, opprettet_timestamp)
            VALUES (?, ?, ?, ?, CURRENT TIMESTAMP, 'OVERFORT', CURRENT TIMESTAMP)
            """,
            saksnummer.verdi,
            soknadId,
            2002,
            12345,
        )

        val vedtakIder = vedtakOverføringRepository.finnVedtakIdBidragVedtakForSak(saksnummer, soknadId)

        vedtakIder shouldBe listOf(12345)
    }

    @Test
    fun `finnVedtakIdBidragVedtakForSak returnerer tom liste når vedtakId mangler`() {
        val saksnummer = Saksnummer("555003")
        val soknadId = 88

        jdbcTemplate.update(
            """
            INSERT INTO T_VEDTAK_OVERFORING
                (saksnr, sokn_id, rolle_hist_id, vedtak_id_bidrag_vedtak,
                 vedtak_timestamp_bisys, status, opprettet_timestamp)
            VALUES (?, ?, ?, NULL, CURRENT TIMESTAMP, 'OVERFORT', CURRENT TIMESTAMP)
            """,
            saksnummer.verdi,
            soknadId,
            2003,
        )

        val vedtakIder = vedtakOverføringRepository.finnVedtakIdBidragVedtakForSak(saksnummer, soknadId)

        vedtakIder shouldBe emptyList()
    }
}
