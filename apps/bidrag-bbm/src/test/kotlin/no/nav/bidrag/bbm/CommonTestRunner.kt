package no.nav.bidrag.bbm

import no.nav.bidrag.bbm.utils.TestdataManager
import no.nav.bidrag.bbm.utils.lagTestdataPeriodeBidrag
import no.nav.bidrag.bbm.utils.lageKodeSøknadStatus
import no.nav.bidrag.bbm.utils.lageTestdataSamvær
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate

@AutoConfigureTestRestTemplate
class CommonTestRunner : SpringTestRunner() {
    @Autowired
    lateinit var testdataManager: TestdataManager

    @Autowired
    lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @BeforeEach
    fun initData() {
        testdataManager.rydd()
        testdataManager.lagrePeriodeBidragListe(lagTestdataPeriodeBidrag())
        testdataManager.lagreSamværListe(lageTestdataSamvær())
        testdataManager.lagreKodeSøknadsstatus(lageKodeSøknadStatus())
    }

    @AfterEach
    fun cleanup() {
        testdataManager.rydd()
    }
}
