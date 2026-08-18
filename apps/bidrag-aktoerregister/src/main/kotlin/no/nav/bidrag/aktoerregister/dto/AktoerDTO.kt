package no.nav.bidrag.aktoerregister.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.aktoerregister.dto.enumer.Gradering

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AktoerDTO(

    @param:Schema(description = "Id for aktøren")
    val aktoerId: AktoerIdDTO,

    @param:Schema(description = "Offentlig id for samhandlere. Angis ikke for personer.")
    val offentligId: String? = null,

    @param:Schema(description = "Type offentlig id. F.eks ORG for norske organisasjonsnummere.")
    val offentligIdType: String? = null,

    @param:Schema(description = "Navn for aktøren")
    val navn: NavnDTO? = null,

    @param:Schema(description = "Gradering for aktøren")
    val gradering: Gradering? = null,

    @param:Schema(description = "Aktørens adresse. Angis ikke for personer.")
    val adresse: AdresseDTO? = null,

    @param:Schema(description = "Språkkoden for aktøren.")
    val sprakkode: String? = null,

    @param:Schema(description = "Lister alle aktørens tidligere identer.")
    val tidligereIdenter: List<AktoerIdDTO>? = null,

    @param:Schema(description = "Personens fødselsdato. Settes for alle personer der fødselsdato er kjent.")
    val fodtDato: String? = null,

    @param:Schema(description = "Personens fødselsdato. Settes for alle personer der fødselsdato er kjent.")
    val dodDato: String? = null,

    @param:Schema(description = "Dødsbo for aktøren")
    val dodsbo: DodsboDTO? = null,

    @param:Schema(description = "Aktørens kontonummer.")
    val kontonummer: KontonummerDTO? = null,
)
