package no.nav.bidrag.sak

interface BidragSakProfiles {
    companion object {
        const val LIVE = "live"
        const val INTEGRATION_DB2 = "integration-db2"
        const val INTEGRATION_TEST = "integration-test"
        const val TEST = "test"
    }
}
