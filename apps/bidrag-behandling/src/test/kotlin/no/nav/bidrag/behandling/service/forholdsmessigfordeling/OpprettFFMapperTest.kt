package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.behandling.database.datamodell.Forpleining
import no.nav.bidrag.behandling.utils.testdata.oppretteTestbehandling
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OpprettFFMapperTest {
    @Test
    fun `skal kopiere forpleining til hovedbehandlingen`() {
        // gitt
        val kildebehandling =
            oppretteTestbehandling(
                setteDatabaseider = true,
                inkludereBp = true,
                behandlingstype = TypeBehandling.BIDRAG,
            )
        val hovedbehandling =
            oppretteTestbehandling(
                setteDatabaseider = true,
                inkludereBp = true,
                behandlingstype = TypeBehandling.BIDRAG,
            )

        val underholdskostnad = kildebehandling.underholdskostnader.first()
        val fom = LocalDate.now().minusMonths(6).withDayOfMonth(1)
        val tom = LocalDate.now().minusMonths(2).withDayOfMonth(1).minusDays(1)
        underholdskostnad.forpleining.add(
            Forpleining(
                underholdskostnad = underholdskostnad,
                fom = fom,
                tom = tom,
                beløp = BigDecimal(2500),
            ),
        )
        val underholdskostnaderFør = hovedbehandling.underholdskostnader.toSet()

        // hvis
        underholdskostnad.kopierUnderholdskostnad(hovedbehandling)

        // så
        val kopiert = (hovedbehandling.underholdskostnader - underholdskostnaderFør).singleOrNull()
        kopiert.shouldNotBeNull()
        kopiert.forpleining shouldHaveSize 1
        val kopiertForpleining = kopiert.forpleining.first()
        kopiertForpleining.fom shouldBe fom
        kopiertForpleining.tom shouldBe tom
        kopiertForpleining.beløp shouldBe BigDecimal(2500)
        // Kopien skal peke på den nye underholdskostnaden, ikke på den opprinnelige
        kopiertForpleining.underholdskostnad shouldBe kopiert
    }
}
