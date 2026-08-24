package no.nav.bidrag.dokument.bestilling.bestilling.produksjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.HgUgDto
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.HgUgDtoFromJson
import no.nav.bidrag.dokument.bestilling.model.BehandlingType
import no.nav.bidrag.dokument.bestilling.model.SoknadType
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets

@Component
class HgUgKodeService {
    private final val hgUgList: List<HgUgDto>

    init {
        hgUgList =
            fetchHgUgKodeListFromFile().map {
                val hg = it.hg?.trim()
                val ug = it.ug?.trim()
                HgUgDto(
                    behandlingType = BehandlingType.fromKode(it.behandlingType),
                    soknadFra = SøktAvType.fraKode(it.soknadFra),
                    soknadType = SoknadType.fromKode(it.soknadType),
                    hg = hg?.ifEmpty { null },
                    ug = ug?.ifEmpty { null },
                )
            }
    }

    fun findHgUg(
        soknadType: SoknadType?,
        soknadFra: SøktAvType?,
        behandlingType: BehandlingType?,
    ): HgUgDto? {
        val behandlingTypeConverted = if (behandlingType == BehandlingType.BIDRAG18AAR) BehandlingType.BIDRAG_18_AR else behandlingType
        return hgUgList.find { it.behandlingType == behandlingTypeConverted && it.soknadType == soknadType && it.soknadFra == soknadFra }
    }

    private fun fetchHgUgKodeListFromFile(): List<HgUgDtoFromJson> =
        try {
            val objectMapper = ObjectMapper(YAMLFactory())
            objectMapper.findAndRegisterModules()
            val inputstream = ClassPathResource("files/hg_ug.json").inputStream
            val text = String(inputstream.readAllBytes(), StandardCharsets.UTF_8)
            objectMapper.readValue(
                text,
                objectMapper.typeFactory.constructCollectionType(
                    MutableList::class.java,
                    HgUgDtoFromJson::class.java,
                ),
            )
        } catch (e: IOException) {
            throw RuntimeException("Kunne ikke laste fil", e)
        }
}
