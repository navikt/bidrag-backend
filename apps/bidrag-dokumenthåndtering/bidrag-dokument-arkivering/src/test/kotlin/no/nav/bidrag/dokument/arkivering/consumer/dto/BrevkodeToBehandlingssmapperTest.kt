package no.nav.bidrag.dokument.arkivering.consumer.dto

import no.nav.bidrag.dokument.arkivering.dto.Behandlingstema
import no.nav.bidrag.dokument.arkivering.dto.BrevkodeToBehandlingstemaMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrevkodeToBehandlingssmapperTest {
    var behandlingstemaMapper = BrevkodeToBehandlingstemaMapper()

    @Test
    fun `skal mappe brevkode til OPPFOSTRINGSBIDRAG`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("BI01S50", "BID")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.OPPFOSTRINGSBIDRAG)
    }

    @Test
    fun `skal mappe brevkode til BIDRAG_INKLUSIV_FARSKAP`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("BI01H02", "BID")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.BIDRAG_INKLUSIV_FARSKAP)
    }

    @Test
    fun `skal mappe brevkode til BIDRAG_UTLAND_EKSKLUSIV_FARSKAP`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("BI01B21", "BID")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.BIDRAG_UTLAND_EKSKLUSIV_FARSKAP)
    }

    @Test
    fun `skal mappe brevkode til EKTEFELLE`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("BI01I50", "BID")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.EKTEFELLE)

        val behandlingstema2 = behandlingstemaMapper.toBehandlingstema("BI01S42", "BID")
        assertThat(behandlingstema2).isEqualTo(Behandlingstema.EKTEFELLE)
    }

    @Test
    fun `skal mappe ukjent brevkode til BIDRAG_EKSKLUSIV_FARSKAP`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("UKJENT", "BID")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP)
    }

    @Test
    fun `skal mappe null brevkode til BIDRAG_EKSKLUSIV_FARSKAP`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema(null, null)
        assertThat(behandlingstema).isEqualTo(Behandlingstema.BIDRAG_EKSKLUSIV_FARSKAP)
    }

    @Test
    fun `skal mappe ukjent brevkode med fagområde farskap til BIDRAG_INKLUSIV_FARSKAP`() {
        val behandlingstema = behandlingstemaMapper.toBehandlingstema("UKJENT", "FAR")
        assertThat(behandlingstema).isEqualTo(Behandlingstema.BIDRAG_INKLUSIV_FARSKAP)
    }
}
