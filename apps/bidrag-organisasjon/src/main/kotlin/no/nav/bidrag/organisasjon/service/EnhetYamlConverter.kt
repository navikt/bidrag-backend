package no.nav.bidrag.organisasjon.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.transport.organisasjon.EnhetDetaljerDto
import no.nav.bidrag.transport.organisasjon.EnhetPostadresseDto
import no.nav.bidrag.transport.organisasjon.EnheterGruppeDto
import org.springframework.core.io.ClassPathResource

object EnhetYamlConverter {

    fun hentAlleEnheterGrupper(): MutableList<EnheterGruppeDto> {
        val resource = ClassPathResource("files/enheter.yaml")
        val mapper = ObjectMapper(YAMLFactory())
        val tree = mapper.readTree(resource.inputStream)

        val grupper = mutableListOf<EnheterGruppeDto>()
        val fieldNames = tree.fieldNames()
        while (fieldNames.hasNext()) {
            val gruppeNavn = fieldNames.next()
            val gruppeNode = tree.get(gruppeNavn)
            val enheter = mutableListOf<EnhetDetaljerDto>()
            gruppeNode.fieldNames().forEach { enhetId ->
                val node = gruppeNode.get(enhetId)
                val navn = node.get("navn")?.asText()
                val settekontor = node.get("settekontor")?.asText()
                val sakskategoriNode = node.get("sakskategori")?.asText()
                val sakskategori = sakskategoriNode?.let {
                    try {
                        Sakskategori.valueOf(it)
                    } catch (_: Exception) {
                        null
                    }
                } ?: Sakskategori.NASJONAL
                val telefon = node.get("telefonnummer")?.asText()
                val postadresseNode = node.get("postadresse")
                val postadresse: MutableMap<Språk, EnhetPostadresseDto> = mutableMapOf()
                if (postadresseNode != null && postadresseNode.isObject) {
                    postadresseNode.fieldNames().forEach { langStr ->
                        val langNode = postadresseNode.get(langStr)
                        val sprak = try {
                            Språk.valueOf(langStr)
                        } catch (_: Exception) {
                            null
                        }
                        if (sprak != null && langNode != null && langNode.isObject) {
                            val post = EnhetPostadresseDto(
                                navn = langNode.get("navn")?.asText(navn) ?: navn,
                                adresselinje1 = langNode.get("adresselinje1")?.asText(null),
                                adresselinje2 = langNode.get("adresselinje2")?.asText(null),
                                postnummer = langNode.get("postnr")?.asText(null),
                                poststed = langNode.get("poststed")?.asText(null),
                                land = langNode.get("land")?.asText(null),
                                kommunenr = langNode.get("kommunenr")?.asText(null),
                            )
                            postadresse[sprak] = post
                        }
                    }
                }
                enheter.add(EnhetDetaljerDto(enhetId, navn, sakskategori, settekontor, telefon, postadresse.ifEmpty { emptyMap() }))
            }
            grupper.add(EnheterGruppeDto(gruppeNavn, enheter))
        }
        return grupper
    }
}
