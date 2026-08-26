package no.nav.bidrag.transport.dokumentmaler

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.årsbeløpTilMåndesbeløp
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakResultatInnhold
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DokumentBestilling(
    val bestillBatch: Boolean,
    var mottaker: Mottaker? = null,
    var gjelder: Gjelder? = null,
    var kontaktInfo: EnhetKontaktInfo? = null,
    val saksbehandler: Saksbehandler? = null,
    var dokumentreferanse: String? = null,
    val tittel: String? = null,
    val enhet: String? = null,
    val saksnummer: String? = null,
    val datoSakOpprettet: LocalDate? = null,
    val spraak: String? = null,
    val roller: Roller = Roller(),
    val rollerV2: List<DokumentmalPersonDto> = emptyList(),
    val rmISak: Boolean? = false,
    val vedtakDetaljer: VedtakDetaljer? = null,
    val sjablonDetaljer: SjablonDetaljer,
    val sakDetaljer: SakDetaljer,
)

class Roller : MutableList<Rolle> by mutableListOf() {
    val barn: List<Barn> get() = filterIsInstance<Barn>().sortedBy { it.fodselsdato }
    val bidragsmottaker get() = filterIsInstance<PartInfo>().find { it.rolle == Rolletype.BIDRAGSMOTTAKER }
    val bidragspliktig get() = filterIsInstance<PartInfo>().find { it.rolle == Rolletype.BIDRAGSPLIKTIG }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = PartInfo::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Barn::class, name = "BA"),
)
interface Rolle {
    val type: Rolletype?
    val rolle: Rolletype
    val fodselsnummer: String?
    val navn: String
    val fodselsdato: LocalDate?
}

data class Barn(
    override val type: Rolletype = Rolletype.BARN,
    override val rolle: Rolletype = Rolletype.BARN,
    override val fodselsnummer: String?,
    override val navn: String,
    override val fodselsdato: LocalDate?,
    val fornavn: String? = null,
    val bidragsbelop: Int? = null,
    val forskuddsbelop: Int? = null,
    val gebyrRm: Int? = null,
    val fodselsnummerRm: String? = null,
) : Rolle

data class SoknadsPart(
    val bidragsPliktigInfo: PartInfo? = null,
    val bidragsMottakerInfo: PartInfo? = null,
)

data class PartInfo(
    override val type: Rolletype? = null,
    override var rolle: Rolletype,
    override val fodselsnummer: String? = null,
    override val navn: String,
    override val fodselsdato: LocalDate? = null,
    val doedsdato: LocalDate? = null,
    val landkode: String? = null,
    val landkode3: String? = null,
    val datoDod: LocalDate? = null,
    val gebyr: Number? = null,
    val kravFremAv: String? = null,
) : Rolle

data class EnhetKontaktInfo(
    val navn: String,
    val telefonnummer: String,
    val postadresse: Adresse,
    val enhetId: String,
)

data class Gjelder(
    val fodselsnummer: String,
    val navn: String? = null,
    val adresse: Adresse? = null,
    val rolle: Rolletype?,
)

data class Mottaker(
    val fodselsnummer: String,
    val navn: String,
    val spraak: String,
    val adresse: Adresse?,
    val rolle: Rolletype?,
    val fodselsdato: LocalDate?,
)

data class Adresse(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val adresselinje4: String? = null,
    val bruksenhetsnummer: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    val landkode3: String? = null,
    val land: String? = null,
)

data class PeriodeFraTom(
    val fraDato: LocalDate,
    val tomDato: LocalDate? = null,
)
typealias BeløpFraTil = Pair<BigDecimal, BigDecimal>

fun BeløpFraTil.fraVerdi() = this.first

fun BeløpFraTil.tilVerdi() = this.second

interface DataPeriode {
    val periode: ÅrMånedsperiode

    fun erLik(annen: DataPeriode) = this.kopierTilGenerisk() == annen.kopierTilGenerisk()

    fun kopierTilGenerisk(): DataPeriode
}

data class VedtakPeriode(
    val fomDato: LocalDate,
    val tomDato: LocalDate? = null,
    val beløp: BigDecimal,
    val innkreving: String? = null,
    val resultatKode: String,
    val inntektGrense: BigDecimal,
    val maksInntekt: BigDecimal,
    val inntekter: List<InntektPeriode> = emptyList(),
    val samvær: Samværsperiode?,
    val bidragsevne: BidragsevnePeriode? = null,
    val underhold: UnderholdskostnaderPeriode? = null,
    val andelUnderhold: AndelUnderholdskostnadPeriode? = null,
)

data class AndelUnderholdskostnadPeriode(
    override val periode: ÅrMånedsperiode,
    val inntektBM: BigDecimal? = null,
    val inntektBP: BigDecimal? = null,
    val inntektBarn: BigDecimal? = null,
    val barnEndeligInntekt: BigDecimal? = null,
    val andelFaktor: BigDecimal? = null,
    val beløpUnderholdskostnad: BigDecimal? = null,
    val beløpBpsAndel: BigDecimal,
) : DataPeriode {
    override fun kopierTilGenerisk() =
        copy(periode = ÅrMånedsperiode(LocalDate.now(), null), beløpUnderholdskostnad = BigDecimal.ZERO, beløpBpsAndel = BigDecimal.ZERO)

    val totalEndeligInntekt get() =
        (inntektBM ?: BigDecimal.ZERO) + (inntektBP ?: BigDecimal.ZERO) +
            (barnEndeligInntekt ?: BigDecimal.ZERO)
}

data class UnderholdskostnaderPeriode(
    override val periode: ÅrMånedsperiode,
    val tilsynstype: Tilsynstype?,
    val skolealder: Skolealder?,
    val harBarnetilsyn: Boolean = false,
    val delberegning: DelberegningUnderholdskostnad,
    val gjelderIdent: String,
    val rolletype: Rolletype?,
) : DataPeriode {
    override fun kopierTilGenerisk() = copy(periode = ÅrMånedsperiode(LocalDate.now(), null))
}

data class BidragsevnePeriode(
    val periode: ÅrMånedsperiode,
    val sjabloner: BidragsevneSjabloner,
    val bidragsevne: BigDecimal,
    val beløpBidrag: BigDecimal,
    val harFullEvne: Boolean,
    val harDelvisEvne: Boolean,
    val inntektBP: BigDecimal,
    val underholdEgneBarnIHusstand: UnderholdEgneBarnIHusstand,
    val skatt: Skatt,
    val borMedAndreVoksne: Boolean,
) {
    data class BidragsevneSjabloner(
        val beløpMinstefradrag: BigDecimal,
        val beløpKlassfradrag: BigDecimal,
        val beløpUnderholdEgneBarnIHusstanden: BigDecimal,
        val boutgiftBeløp: BigDecimal,
        val underholdBeløp: BigDecimal,
    )
}

data class Samværsperiode(
    override val periode: ÅrMånedsperiode,
    val samværsklasse: Samværsklasse,
    val aldersgruppe: Pair<Int, Int?>?,
    val samværsfradragBeløp: BigDecimal,
) : DataPeriode {
    override fun kopierTilGenerisk() = copy(periode = ÅrMånedsperiode(LocalDate.now(), null))
}

data class InntektPeriode(
    val inntektPerioder: Set<ÅrMånedsperiode> = emptySet(),
    val inntektOpprinneligPerioder: Set<ÅrMånedsperiode> = emptySet(),
    override val periode: ÅrMånedsperiode,
    val typer: Set<Inntektsrapportering> = emptySet(),
    val periodeTotalinntekt: Boolean? = false,
    val nettoKapitalInntekt: Boolean? = false,
    val beløpÅr: Int? = null,
    val fødselsnummer: String?,
    val beløp: BigDecimal,
    val rolle: Rolletype,
    val innteksgrense: BigDecimal,
) : DataPeriode {
    val type get() = typer.firstOrNull()

    override fun kopierTilGenerisk() = copy()
}

data class Saksbehandler(
    val ident: String? = null,
    val navn: String? = null,
) {
    val fornavnEtternavn: String get() =
        if (navn.isNullOrEmpty()) {
            navn ?: ""
        } else {
            val navnDeler = navn.split(",\\s*".toRegex(), limit = 2).toTypedArray()
            if (navnDeler.size > 1) {
                navnDeler[1] + " " + navnDeler[0]
            } else {
                navn
            }
        }
}

data class ForskuddInntektgrensePeriode(
    val fomDato: LocalDate,
    val tomDato: LocalDate? = null,
    val forsorgerType: Sivilstandskode,
    val antallBarn: Int,
    val beløp50Prosent: BeløpFraTil,
    val beløp75Prosent: BeløpFraTil,
)

data class VedtakDetaljer(
    val årsakKode: VirkningstidspunktÅrsakstype?,
    val avslagsKode: Resultatkode?,
    val type: TypeBehandling,
    val gebyr: GebyrInfoDto? = null,
    val virkningstidspunkt: LocalDate?,
    val mottattDato: LocalDate?,
    val soktFraDato: LocalDate?,
    val vedtattDato: LocalDate? = null,
    val saksbehandlerInfo: VedtakSaksbehandlerInfo,
    val vedtakstype: Vedtakstype,
    val stønadstype: Stønadstype?,
    val engangsbeløptype: Engangsbeløptype?,
    val søknadFra: SøktAvType? = null,
    val kilde: Vedtakskilde = Vedtakskilde.MANUELT,
    val vedtakBarn: List<VedtakBarn> = emptyList(),
    val resultat: List<VedtakResultatInnhold>,
    val barnIHusstandPerioder: List<BarnIHusstandPeriode> = emptyList(),
    val sivilstandPerioder: List<SivilstandPeriode> = emptyList(),
) {
    val erDirekteAvslagForAlleBarn: Boolean get() = vedtakBarn.all { it.erDirekteAvslag }

    fun hentForskuddBarn(fodselsnummer: String): BigDecimal? =
        vedtakBarn
            .find { it.fødselsnummer == fodselsnummer }
            ?.stønadsendringer
            ?.find { it.type == Stønadstype.FORSKUDD }
            ?.vedtakPerioder
            ?.find { it.tomDato == null }
            ?.beløp
}

data class GebyrInfoDto(
    val bmGebyr: BigDecimal? = null,
    val bpGebyr: BigDecimal? = null,
)

data class BarnIHusstandPeriode(
    val periode: ÅrMånedsperiode,
    val antall: Double,
)

data class VedtakBarn(
    val fødselsnummer: String,
    val navn: String?,
    val sumAvregning: BigDecimal,
    val løpendeBidrag: BigDecimal? = null,
    val bostatusPerioder: List<BostatusPeriode>,
    val stønadsendringer: List<VedtakBarnStonad> = emptyList(),
    val engangsbeløper: List<VedtakBarnEngangsbeløp> = emptyList(),
) {
    val erDirekteAvslag get() = stønadsendringer.all { it.direkteAvslag }
}

data class VedtakBarnEngangsbeløp(
    val type: Engangsbeløptype,
    val sjablon: BrevSjablonVerdier,
    val periode: Datoperiode,
    val medInnkreving: Boolean,
    val erDirekteAvslag: Boolean,
    val særbidragBeregning: SærbidragBeregning? = null,
    val inntekter: List<InntektPeriode> = emptyList(),
)

data class VedtakPeriodeReferanse(
    val periode: ÅrMånedsperiode,
    val resultatKode: Resultatkode?,
    val typeBehandling: TypeBehandling,
    val grunnlagReferanseListe: List<Grunnlagsreferanse> = emptyList(),
) {
    constructor(
        periode: Datoperiode,
        type: TypeBehandling,
        grunnlagReferanseListe: List<Grunnlagsreferanse>,
    ) : this(ÅrMånedsperiode(periode.fom, periode.til), null, type, grunnlagReferanseListe)
}

data class BrevSjablonVerdier(
    val forskuddSats: BigDecimal,
    val inntektsgrense: BigDecimal,
)

data class SærbidragBeregning(
    val kravbeløp: BigDecimal = BigDecimal.ZERO,
    val godkjentbeløp: BigDecimal = BigDecimal.ZERO,
    val resultat: BigDecimal = BigDecimal.ZERO,
    val resultatKode: Resultatkode,
    val beløpDirekteBetaltAvBp: BigDecimal = BigDecimal.ZERO,
    val andelProsent: BigDecimal = BigDecimal.ZERO,
    val inntekt: Inntekt = Inntekt(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
) {
    data class Inntekt(
        val bmInntekt: BigDecimal,
        val bpInntekt: BigDecimal,
        val barnInntekt: BigDecimal,
        val totalInntekt: BigDecimal = barnInntekt + bmInntekt + bpInntekt,
    )
}

data class VedtakBarnStonad(
    val type: Stønadstype,
    val innkreving: Boolean,
    val direkteAvslag: Boolean,
    val vedtakPerioder: List<VedtakPeriode> = emptyList(),
    val forskuddInntektgrensePerioder: List<ForskuddInntektgrensePeriode> = emptyList(),
)

data class SjablonDetaljer(
    val multiplikatorInntekstgrenseForskudd: BigDecimal,
    val fastsettelseGebyr: BigDecimal,
    val forskuddInntektIntervall: BigDecimal,
    val forskuddSats: BigDecimal,
    val inntektsintervallTillegsbidrag: BigDecimal,
    val multiplikatorHøyInntektBp: BigDecimal,
    val multiplikatorMaksBidrag: BigDecimal,
    val multiplikatorInnteksinslagBarn: BigDecimal,
    val multiplikatorMaksInntekBarn: BigDecimal,
    val nedreInntekstgrenseGebyr: BigDecimal,
    val prosentsatsTilleggsbidrag: BigDecimal,
    val maksProsentAvInntektBp: BigDecimal,
    val forskuddInntektgrensePerioder: List<ForskuddInntektgrensePeriode> = emptyList(),
    val maksgrenseHøyInntekt: BigDecimal = forskuddSats * multiplikatorHøyInntektBp,
    val maksBidragsgrense: BigDecimal = forskuddSats * multiplikatorMaksBidrag,
    val maksInntektsgrense: BigDecimal = forskuddSats * multiplikatorMaksInntekBarn,
    val maksForskuddsgrense: BigDecimal = forskuddSats * multiplikatorInntekstgrenseForskudd,
    val maksInntektsgebyr: BigDecimal = forskuddSats * nedreInntekstgrenseGebyr,
)

data class SakDetaljer(
    val harUkjentPart: Boolean,
    val levdeAdskilt: Boolean,
)

data class VedtakSaksbehandlerInfo(
    val navn: String,
    val ident: String,
)

data class UnderholdEgneBarnIHusstand(
    val årsbeløp: BigDecimal,
    val sjablon: BigDecimal,
    val antallBarnIHusstanden: Double,
    val antallBarnDeltBossted: Int,
) {
    val måndesbeløp get() = årsbeløp.årsbeløpTilMåndesbeløp()
}

data class Skatt(
    val sumSkattFaktor: BigDecimal,
    val sumSkatt: BigDecimal,
    val skattAlminneligInntekt: BigDecimal,
    val trinnskatt: BigDecimal,
    val trygdeavgift: BigDecimal,
) {
    val skattMånedsbeløp get() = sumSkatt.årsbeløpTilMåndesbeløp()
    val trinnskattMånedsbeløp get() = trinnskatt.årsbeløpTilMåndesbeløp()
    val skattAlminneligInntektMånedsbeløp get() = skattAlminneligInntekt.årsbeløpTilMåndesbeløp()
    val trygdeavgiftMånedsbeløp get() = trygdeavgift.årsbeløpTilMåndesbeløp()
}

data class DelvedtakDto(
    val type: Vedtakstype?,
    val omgjøringsvedtak: Boolean,
    val vedtaksid: Int? = null,
    val delvedtak: Boolean,
    val beregnet: Boolean,
    val indeksår: Int,
    val resultatFraVedtakVedtakstidspunkt: LocalDateTime? = null,
    val perioder: List<DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto>,
    val grunnlagFraVedtak: List<GrunnlagFraVedtak> = emptyList(),
) {
    val endeligVedtak get() = !omgjøringsvedtak && !delvedtak
}

data class GrunnlagFraVedtak(
    @Schema(
        description =
            "Årstall for aldersjustering av grunnlag. " +
                "Brukes hvis det er et vedtak som skal brukes for aldersjustering av grunnlag. " +
                "Dette er relevant ved omgjøring/klagebehanding i bidrag ellers aldersjusteres det for inneværende år eller ikke er relevant",
    )
    val aldersjusteringForÅr: Int? = null,
    val vedtak: Int? = null,
    val grunnlagFraOmgjøringsvedtak: Boolean = false,
    val vedtakstidspunkt: LocalDateTime? = null,
    @Schema(
        description =
            "Perioder i vedtaket som er valgt. " +
                "Brukes når vedtakstype er innkreving og det er valgt å innkreve en vedtak fra NAV som opprinnelig var uten innkreving",
    )
    val perioder: List<VedtakPeriodeDto> = emptyList(),
)
