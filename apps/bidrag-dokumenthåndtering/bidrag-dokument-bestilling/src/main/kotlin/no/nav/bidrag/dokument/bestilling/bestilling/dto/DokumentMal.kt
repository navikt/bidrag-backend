package no.nav.bidrag.dokument.bestilling.bestilling.dto

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.nio.charset.StandardCharsets

object EnhetKode {
    const val FARSKAP = "4860"
    const val UTLAND = "4865"
    const val EGENANSATT = "4883"
}

object BestillingSystem {
    const val BREVSERVER = "BREVSERVER"
    const val DOKUMENT_PRODUKSJON = "DOKUMENT_PRODUKSJON"
    const val BUCKET = "BUCKET"
}

enum class DokumentType {
    UTGÅENDE,
    NOTAT,
}

enum class DokumentMalType {
    NOTAT,

    // Varselbrev som ikke er knyttet til en behandling
    VARSEL_STANDARD,
    VARSEL,
    VEDTAK,
    VEDLEGG_VEDTAK,
    VEDLEGG_VARSEL,
    SKJEMA,
}
typealias BestillingSystemType = String

enum class StøttetSpråk {
    NB,
    EN,
    NN,
    DE,
    PL,
    FR,
    ES,
}

abstract class DokumentMal(
    open val kode: String,
    open val beskrivelse: String,
    open val tittel: String,
    open val bestillingSystem: BestillingSystemType,
    open val batchbrev: Boolean,
    open val enabled: Boolean,
    open val avslagsbrev: Boolean = false,
    open val kreverEkstraData: List<DataGrunnlag> = emptyList(),
    open val type: DokumentMalType,
    open val redigerbar: Boolean,
) {
    val kreverDataGrunnlag get() =
        !listOf(DokumentMalType.NOTAT, DokumentMalType.VEDLEGG_VARSEL, DokumentMalType.VEDLEGG_VEDTAK, DokumentMalType.SKJEMA).contains(type) ||
            kreverEkstraData.isNotEmpty()

    fun inneholderDatagrunnlag(dataGrunnlag: DataGrunnlag) =
        kreverEkstraData.contains(dataGrunnlag).takeIf { it }
            ?: when (dataGrunnlag) {
                DataGrunnlag.VEDTAK -> type == DokumentMalType.VEDTAK
                DataGrunnlag.BEHANDLING -> type == DokumentMalType.VARSEL
                DataGrunnlag.ROLLER -> listOf(DokumentMalType.VARSEL, DokumentMalType.VEDTAK, DokumentMalType.VARSEL_STANDARD).contains(type)
                DataGrunnlag.ENHET_KONTAKT_INFO -> listOf(DokumentMalType.VARSEL, DokumentMalType.VEDTAK, DokumentMalType.VARSEL_STANDARD).contains(type)
                else -> kreverEkstraData.contains(dataGrunnlag)
            }
}

enum class DataGrunnlag {
    VEDTAK,
    BEHANDLING,
    SJABLON,
    ROLLER,
    ENHET_KONTAKT_INFO,
}

data class DokumentMalBucketUtland(
    override val folderName: String = "vedlegg_utland",
    override val kode: String,
    override val beskrivelse: String,
    override val språk: StøttetSpråk,
    override val tittel: String = beskrivelse,
    override val tilhørerEnheter: List<String> = listOf(EnhetKode.UTLAND, EnhetKode.EGENANSATT),
    override val type: DokumentMalType = DokumentMalType.VARSEL,
    override val gruppeVisningsnavn: String,
) : DokumentMalBucket(
    kode = kode,
    beskrivelse = beskrivelse,
    folderName = folderName,
    tittel = tittel,
    tilhørerEnheter = tilhørerEnheter,
    språk = språk,
    type = type,
    gruppeVisningsnavn = gruppeVisningsnavn,
)

data class DokumentMalBucketFarskap(
    override val folderName: String = "vedlegg_farskap",
    override val kode: String,
    override val tittel: String,
    override val beskrivelse: String = tittel,
    override val tilhørerEnheter: List<String> = listOf(EnhetKode.FARSKAP),
    override val type: DokumentMalType = DokumentMalType.VARSEL,
    override val gruppeVisningsnavn: String,
) : DokumentMalBucket(
    kode = kode,
    beskrivelse = beskrivelse,
    folderName = folderName,
    tittel = tittel,
    tilhørerEnheter = tilhørerEnheter,
    type = type,
    gruppeVisningsnavn = gruppeVisningsnavn,
)

open class DokumentMalBucket(
    override val kode: String,
    override val beskrivelse: String,
    override val tittel: String,
    override val batchbrev: Boolean = false,
    override val enabled: Boolean = true,
    override val redigerbar: Boolean = false,
    override val type: DokumentMalType = DokumentMalType.SKJEMA,
    override val bestillingSystem: BestillingSystemType = BestillingSystem.BUCKET,
    open val folderName: String,
    open val filsti: String? = null,
    open val tilhørerEnheter: List<String> = emptyList(),
    open val språk: StøttetSpråk = StøttetSpråk.NB,
    open val gruppeVisningsnavn: String? = null,
) : DokumentMal(
    kode = kode,
    beskrivelse = beskrivelse,
    tittel = tittel,
    bestillingSystem = bestillingSystem,
    batchbrev = batchbrev,
    enabled = enabled,
    type = type,
    redigerbar = redigerbar,
) {
    private val bucketFilename get() = filsti?.substringBefore(".pdf") ?: kode
    val filePath get() = "$folderName/$bucketFilename.pdf"
}

data class DokumentMalProduksjon(
    override val kode: String,
    override val tittel: String,
    override val beskrivelse: String = tittel,
    override val batchbrev: Boolean = false,
    override val enabled: Boolean = true,
    override val redigerbar: Boolean = false,
    override val type: DokumentMalType = DokumentMalType.VARSEL,
    override val bestillingSystem: BestillingSystemType = BestillingSystem.DOKUMENT_PRODUKSJON,
    val støttetSpråk: List<StøttetSpråk> = listOf(StøttetSpråk.NB),
    val forOrkestrertVedtak: Boolean = false,
    val innhold: InnholdType = InnholdType.FORSIDE,
    val malId: String,
) : DokumentMal(
    kode = kode,
    beskrivelse = beskrivelse,
    tittel = tittel,
    bestillingSystem = bestillingSystem,
    batchbrev = batchbrev,
    enabled = enabled,
    type = type,
    redigerbar = redigerbar,
) {
    enum class InnholdType {
        FORSIDE,
    }
}

data class DokumentMalBrevserver(
    override val kode: String,
    override val tittel: String,
    override val beskrivelse: String = tittel,
    override val batchbrev: Boolean = false,
    override val enabled: Boolean = false,
    override val redigerbar: Boolean = !batchbrev,
    override val type: DokumentMalType = DokumentMalType.VARSEL,
    override val bestillingSystem: BestillingSystemType = BestillingSystem.BREVSERVER,
    val støttetSpråk: List<StøttetSpråk> = listOf(StøttetSpråk.NB),
) : DokumentMal(
    kode = kode,
    beskrivelse = beskrivelse,
    tittel = tittel,
    bestillingSystem = bestillingSystem,
    batchbrev = batchbrev,
    enabled = enabled,
    type = type,
    redigerbar = redigerbar,
)

enum class FilType {
    JSON,
    YAML,
}

private inline fun <reified T : DokumentMal> lastDokumentMalerFraFil(
    filnavn: String,
    prefiks: String? = null,
    type: FilType = FilType.JSON,
    groupname: String? = null,
): List<T> =
    try {
        val fileending = if (type == FilType.JSON) "json" else "yaml"
        val objectMapper = ObjectMapper(YAMLFactory())
        objectMapper
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val inputstream = ClassPathResource("files/dokumentmaler/$filnavn.$fileending").inputStream
        val text = String(inputstream.readAllBytes(), StandardCharsets.UTF_8)
        val textConverted =
            if (groupname != null) {
                konverterGruppeNavnTilParameter(text, groupname)
            } else {
                text
            }
        val textWithPrefiks =
            if (prefiks != null) {
                val json = objectMapper.readTree(textConverted)
                json.asSequence().forEach {
                    (it as ObjectNode).put("kode", "${prefiks}_${it.get("kode").asText()}")
                }
                json.toString()
            } else {
                textConverted
            }

    val listType: JavaType =
        objectMapper.typeFactory.constructCollectionType(MutableList::class.java, T::class.java)
    val dokumentmalerMap: List<T> =
        objectMapper.readValue(
            textWithPrefiks,
            listType,
        )

    dokumentmalerMap.map { it }
} catch (e: IOException) {
    throw RuntimeException("Kunne ikke laste fil", e)
}

private fun konverterGruppeNavnTilParameter(
    payload: String,
    parameterName: String,
): String =
    try {
        val objectMapper = ObjectMapper(YAMLFactory())
        objectMapper
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        val mapType: JavaType =
            objectMapper.typeFactory.constructMapType(
                Map::class.java,
                String::class.java,
                ArrayNode::class.java,
            )
        val payloadMap: Map<String, ArrayNode> =
            objectMapper.readValue(
                payload,
                mapType,
            )

    objectMapper.writeValueAsString(
        payloadMap.keys.flatMap { key ->
            payloadMap[key]?.map {
                (it as ObjectNode).put(parameterName, key)
            } ?: emptyList()
        },
    )
} catch (e: IOException) {
    throw RuntimeException("Kunne ikke laste fil", e)
}

val dokumentmalerBrevserver: List<DokumentMalBrevserver> = lastDokumentMalerFraFil("brevserver")
val dokumentmalerUtland: List<DokumentMalBucketUtland> =
    lastDokumentMalerFraFil("vedlegg_utland", "UTLAND", type = FilType.YAML, "gruppeVisningsnavn")
val dokumentmalerFarskap: List<DokumentMalBucketFarskap> =
    lastDokumentMalerFraFil("vedlegg_farskap", "FARSKAP", type = FilType.YAML, "gruppeVisningsnavn")
val dokumentmalerProduksjon: List<DokumentMalProduksjon> =
    lastDokumentMalerFraFil("vedtak", null, type = FilType.YAML, "innhold")
val alleDokumentmaler = dokumentmalerBrevserver + dokumentmalerUtland + dokumentmalerFarskap + dokumentmalerProduksjon

fun hentDokumentMal(kode: String): DokumentMal? = alleDokumentmaler.find { it.kode == kode }
