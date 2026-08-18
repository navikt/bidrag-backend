package no.nav.bidrag.tilgangskontroll.tjeneste

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.tilgangskontroll.model.kodeverk.BehandlingstemaAdgruppe
import no.nav.bidrag.tilgangskontroll.model.kodeverk.Informasjonstilgang
import no.nav.bidrag.tilgangskontroll.model.kodeverk.InformasjonstilgangAdgruppe
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

private val LOGGER = KotlinLogging.logger {}

@Service
class KodeverkService {
    private val yamlMapper = YAMLMapper.builder().addModule(kotlinModule()).build()
    private lateinit var behandlingstemaMap: Map<String, BehandlingstemaAdgruppe>
    private lateinit var informasjonstilgangerMap: Map<String, InformasjonstilgangAdgruppe>

    companion object {
        const val BEHANDLINGSTEMA_FIL = "kodeverk/behandlingstemaer.yaml"
        const val INFORMASJONSTILGANG_FIL = "kodeverk/informasjonstilganger.yaml"
    }

    @PostConstruct
    fun init() {
        lasteKodeverk()
        validerKodeverk()
    }

    private fun lasteKodeverk() {
        try {
            behandlingstemaMap =
                yamlMapper.readValue<Map<String, BehandlingstemaAdgruppe>>(ClassPathResource(BEHANDLINGSTEMA_FIL).inputStream)
            informasjonstilgangerMap =
                yamlMapper.readValue<Map<String, InformasjonstilgangAdgruppe>>(ClassPathResource(INFORMASJONSTILGANG_FIL).inputStream)
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved lasting av .yaml filer til kodeverk!" }
            throw IllegalStateException("Kunne ikke laste .yaml fil", e)
        }
    }

    private fun validerKodeverk() {
        // Behandlingstema
        val manglendeTemaer =
            Behandlingstema.entries.filter { behandlingstema ->
                !behandlingstemaMap.containsKey(behandlingstema.name)
            }

        if (manglendeTemaer.isNotEmpty()) {
            val feilmelding = "Følgende behandlingstemaer mangler i YAML-filen: ${manglendeTemaer.joinToString()}"
            LOGGER.error { feilmelding }
            throw IllegalStateException(feilmelding)
        }

        // Informasjonstilganger
        val manglendeInformasjonstilganger =
            Informasjonstilgang.entries.filter { informasjonstilgang ->
                !informasjonstilgangerMap.containsKey(informasjonstilgang.name)
            }

        if (manglendeInformasjonstilganger.isNotEmpty()) {
            val feilmelding = "Følgende informasjonstilganger mangler i YAML-filen: ${manglendeInformasjonstilganger.joinToString()}"
            LOGGER.error { feilmelding }
            throw IllegalStateException(feilmelding)
        }
    }

    fun hentAdgruppe(behandlingstema: Behandlingstema): String = behandlingstemaMap[behandlingstema.name]?.adgruppe
        ?: error("Finner ikke AD-gruppe for behandlingstema: ${behandlingstema.name}")

    fun hentAdgruppe(informasjonstilgang: Informasjonstilgang): String = informasjonstilgangerMap[informasjonstilgang.name]?.adgruppe
        ?: error("Finner ikke AD-gruppe for informasjonstilgang: ${informasjonstilgang.name}")
}
