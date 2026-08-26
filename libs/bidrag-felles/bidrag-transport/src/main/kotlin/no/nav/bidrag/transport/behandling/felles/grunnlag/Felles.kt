@file:Suppress("unused")

package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagDatakilde
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.felles.commonObjectmapper
import java.time.LocalDate
import java.time.LocalDateTime

val NULL_PERIODE_FRA = LocalDate.of(0, 1, 1)
typealias Grunnlagsreferanse = String

@Schema(description = "Grunnlag")
data class GrunnlagDto(
    override val referanse: String,
    override val type: Grunnlagstype,
    override val innhold: JsonNode,
    override val grunnlagsreferanseListe: List<Grunnlagsreferanse> = emptyList(),
    override val gjelderReferanse: Grunnlagsreferanse? = null,
    override val gjelderBarnReferanse: Grunnlagsreferanse? = null,
) : BaseGrunnlag {
    override fun toString(): String = super.asString()

    override fun hashCode(): Int = referanse.hashCode() + type.hashCode() + innholdString.hashCode()

    @get:JsonIgnore
    val innholdString get() = commonObjectmapper.writeValueAsString(innhold)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GrunnlagDto) return false
        if (referanse != other.referanse) return false
        if (type != other.type) return false
        if (innholdString != other.innholdString) return false
        return true
    }
}

@Schema(description = "BaseGrunnlag")
interface BaseGrunnlag {
    @get:Schema(description = "Referanse (unikt navn på grunnlaget)")
    val referanse: String

    @get:Schema(description = "Grunnlagstype")
    val type: Grunnlagstype

    @get:Schema(description = "Grunnlagsinnhold (generisk)", type = "GrunnlagInnhold")
    val innhold: JsonNode

    @get:Schema(description = "Referanse til personobjektet grunnlaget gjelder")
    val gjelderReferanse: Grunnlagsreferanse?

    @get:Schema(description = "Referanse til barn personobjektet grunnlaget gjelder")
    val gjelderBarnReferanse: Grunnlagsreferanse?

    @get:Schema(description = "Liste over grunnlagsreferanser")
    val grunnlagsreferanseListe: List<Grunnlagsreferanse>

    fun asString(): String =
        "$type - ${::referanse.name}=$referanse, ${::gjelderReferanse.name}=$gjelderReferanse, " +
            "${::grunnlagsreferanseListe.name}=${grunnlagsreferanseListe.ifEmpty { listOf("<tomt>") }.joinToString(",")}, " +
            "${::innhold.name}=${commonObjectmapper.writeValueAsString(innhold)}"

    fun valider() {
        require(innhold.asText() != "null" && !innhold.isNull) { "innhold kan ikke være null" }
        require(referanse.isNotEmpty()) { "referanse kan ikke være en tom streng" }
    }
}

interface GrunnlagInnhold

interface GrunnlagBeregningPeriode : GrunnlagInnhold {
    val periode: ÅrMånedsperiode
}

interface Delberegning : GrunnlagBeregningPeriode

interface DelberegningUtenPeriode : GrunnlagInnhold

interface Sluttberegning : GrunnlagBeregningPeriode

interface GrunnlagPeriodeInnhold : GrunnlagInnhold {
    val periode: ÅrMånedsperiode

    @get:Schema(
        description =
            "Om grunnlaget er manuelt registrert av saksbehandler " +
                "eller om det er innhentet fra ekstern kilde (skatt/folkregisteret...)",
    )
    val manueltRegistrert: Boolean
}

interface GrunnlagPeriodeInnholdKopi : GrunnlagInnhold {
    val periode: ÅrMånedsperiode
    val fraVedtakId: Int
}

interface InnhentetGrunnlagInnhold<out T> : GrunnlagInnhold {
    @get:Schema(description = "Tidspunkt data hentet fra kilden")
    val hentetTidspunkt: LocalDateTime
    val datakilde: GrunnlagDatakilde
    val grunnlag: T
}

interface InnhentetGrunnlagPeriodeInnhold<out T> : InnhentetGrunnlagInnhold<T> {
    val periode: Datoperiode
}

@Schema(description = "Informasjon om en person som er inkludert i vedtaket")
data class Person(
    val ident: Personident? = null,
    val navn: String? = null,
    val fødselsdato: LocalDate = LocalDate.parse("2000-01-01"),
    val bidragsmottaker: Grunnlagsreferanse? = null,
    val delAvOpprinneligBehandling: Boolean = true,
    @Schema(description = "Stønadstype personen tilhører. Brukes når samme person har både ordinær og 18 års bidrag i samme vedtak")
    val stønadstype: Stønadstype? = null,
) : GrunnlagInnhold
