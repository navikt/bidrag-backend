package no.nav.bidrag.arbeidsflyt.model

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøknadMapperTest {
    @Test
    fun `krever søknadsoppgave når minst en partsøknad krever oppgave`() {
        søknad(
            PartISøknad(
                personident = "12345678901",
                rolletype = Rolletype.BARN,
                behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
            ),
        ).kreverSøknadsoppgave shouldBe true
    }

    @Test
    fun `krever ikke søknadsoppgave når ingen partsøknad krever oppgave`() {
        søknad(
            PartISøknad(
                personident = "12345678901",
                rolletype = Rolletype.BARN,
                behandlingstatus = Behandlingstatus.VEDTAK_FATTET,
            ),
        ).kreverSøknadsoppgave shouldBe false
    }

    @Test
    fun `krever ikke søknadsoppgave når søknaden ikke har partsøknader`() {
        søknad().kreverSøknadsoppgave shouldBe false
    }

    private fun søknad(vararg parts: PartISøknad) = HentSøknad(
        søknadsid = 123L,
        søknadMottattDato = LocalDate.parse("2026-01-01"),
        behandlingstema = Behandlingstema.BIDRAG,
        saksnummer = "123456",
        innkreving = true,
        søktAvType = SøktAvType.BIDRAGSMOTTAKER,
        behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
        partISøknadListe = parts.toList(),
    )
}
