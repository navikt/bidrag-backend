package no.nav.bidrag.transport.dokumentmaler.notat

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvvisning
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorFerietype
import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorNetterFrekvens
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.særbidrag.Utgiftstype
import no.nav.bidrag.domene.enums.vedtak.BeregnTil
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.tid.DatoperiodeDto
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.bidrag.domene.util.visningsnavnIntern
import no.nav.bidrag.domene.util.visningsnavnMedÅrstall
import no.nav.bidrag.transport.behandling.beregning.samvær.SamværskalkulatorDetaljer
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBoforhold
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUtgift
import no.nav.bidrag.transport.dokumentmaler.DokumentmalDelberegningBidragsevneDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalManuellVedtak
import no.nav.bidrag.transport.dokumentmaler.DokumentmalPersonDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBeregningInntekterDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto
import no.nav.bidrag.transport.dokumentmaler.tilVisningsnavn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

data class VedtakNotatDto(
    val type: NotatMalType = NotatMalType.FORSKUDD,
    val erOrkestrertVedtak: Boolean,
    val stønadstype: Stønadstype? = null,
    val medInnkreving: Boolean = true,
    val saksnummer: String,
    val behandling: NotatBehandlingDetaljerDto,
    val saksbehandlerNavn: String?,
    val virkningstidspunkt: NotatVirkningstidspunktDto,
    val utgift: NotatSærbidragUtgifterDto?,
    val boforhold: NotatBoforholdDto,
    val samvær: List<NotatSamværBarnDto> = emptyList(),
    val samværV2: NotatSamværDto? = null,
    val gebyr: List<NotatGebyrDetaljerDto>? = null,
    val gebyrV2: NotatGebyrV2Dto? = null,
    val gebyrV3: NotatGebyrV3Dto? = null,
    var underholdskostnader: NotatUnderholdDto? = null,
    val personer: List<DokumentmalPersonDto>,
    val privatavtale: List<NotatPrivatAvtaleDto>,
    val roller: List<DokumentmalPersonDto> = personer,
    val inntekter: NotatInntekterDto,
    val vedtak: NotatVedtakDetaljerDto,
)

@Schema(enumAsRef = true)
enum class ResultatInnholdType {
    FORSKUDD,
    SÆRBIDRAG,
    BIDRAG,
    GEBYR,
}

@Schema(enumAsRef = true)
enum class NotatMalType {
    FORSKUDD,
    SÆRBIDRAG,
    BIDRAG,
}

data class NotatBehandlingDetaljerDto(
    val klageMottattDato: LocalDate? = null,
    val vedtakstype: Vedtakstype?,
    val opprinneligVedtakstype: Vedtakstype? = null,
    val kategori: NotatSærbidragKategoriDto? = null,
    val søktAv: SøktAvType?,
    val innkreving: Boolean = true,
    val mottattDato: LocalDate?,
    val søktFraDato: YearMonth?,
    val søknadstype: String?,
    @Deprecated("Hent informasjon fra virkningstidspunkt")
    val virkningstidspunkt: LocalDate?,
    @Deprecated("Hent informasjon fra virkningstidspunkt")
    val avslag: Resultatkode?,
) {
    @get:Schema(name = "erAvvisning")
    val erAvvisning get() = avslag?.erAvvisning() == true

    @get:Schema(name = "avslagVisningsnavn")
    val avslagVisningsnavn
        get() = vedtakstype?.let { avslag?.visningsnavnIntern(vedtakstype) } ?: avslag?.visningsnavn?.intern

    @get:Schema(name = "avslagVisningsnavnUtenPrefiks")
    val avslagVisningsnavnUtenPrefiks
        get() = avslag?.visningsnavn?.intern

    @get:Schema(name = "kategoriVisningsnavn")
    val kategoriVisningsnavn
        get() =
            if (kategori?.kategori == Særbidragskategori.ANNET) {
                kategori.kategori.visningsnavn.intern + ": " + kategori.beskrivelse
            } else {
                kategori?.kategori?.visningsnavn?.intern
            }

    @get:Schema(name = "vedtakstypeVisningsnavn")
    val vedtakstypeVisningsnavn get() = vedtakstype?.visningsnavnIntern(opprinneligVedtakstype)
}

data class NotatUnderholdDto(
    val underholdskostnaderBarn: List<NotatUnderholdBarnDto> = emptyList(),
    val offentligeOpplysninger: List<NotatOffentligeOpplysningerUnderholdBarn> = emptyList(),
    val offentligeOpplysningerV2: NotatOffentligeOpplysningerUnderhold =
        NotatOffentligeOpplysningerUnderhold(
            offentligeOpplysninger,
            emptyList(),
            offentligeOpplysninger.any { it.harTilleggsstønad },
        ),
)

data class NotatOffentligeOpplysningerUnderhold(
    val offentligeOpplysningerBarn: List<NotatOffentligeOpplysningerUnderholdBarn> = emptyList(),
    val andreBarnTilBidragsmottaker: List<DokumentmalPersonDto> = emptyList(),
    val bidragsmottakerHarInnvilgetTilleggsstønad: Boolean,
)

data class NotatOffentligeOpplysningerUnderholdBarn(
    val gjelder: DokumentmalPersonDto,
    val gjelderBarn: DokumentmalPersonDto? = null,
    val barnetilsyn: List<NotatBarnetilsynOffentligeOpplysninger> = emptyList(),
    val harTilleggsstønad: Boolean,
) {
    data class NotatBarnetilsynOffentligeOpplysninger(
        val periode: ÅrMånedsperiode,
        val tilsynstype: Tilsynstype? = null,
        val skolealder: Skolealder? = null,
    )
}

data class NotatUnderholdBarnDto(
    val gjelderBarn: DokumentmalPersonDto,
    val harTilsynsordning: Boolean? = null,
    val stønadTilBarnetilsyn: List<NotatStønadTilBarnetilsynDto> = emptyList(),
    val faktiskTilsynsutgift: List<NotatFaktiskTilsynsutgiftDto>,
    val tilleggsstønad: List<NotatTilleggsstønadDto> = emptyList(),
    val underholdskostnad: List<NotatUnderholdskostnadBeregningDto>,
    val begrunnelse: NotatBegrunnelseDto? = null,
) {
    data class NotatUnderholdskostnadPeriodeBeregningsdetaljer(
        val tilsynsutgifterBarn: List<NotatTilsynsutgiftBarn> = emptyList(),
        val sjablonMaksTilsynsutgift: BigDecimal,
        val sjablonMaksFradrag: BigDecimal,
        val antallBarnBMUnderTolvÅr: Int,
        val antallBarnBMBeregnet: Int = antallBarnBMUnderTolvÅr,
        val antallBarnMedTilsynsutgifter: Int = antallBarnBMBeregnet,
        val skattesatsFaktor: BigDecimal,
        val totalTilsynsutgift: BigDecimal,
        val sumTilsynsutgifter: BigDecimal,
        val bruttoTilsynsutgift: BigDecimal,
        val justertBruttoTilsynsutgift: BigDecimal,
        val nettoTilsynsutgift: BigDecimal,
        val erBegrensetAvMaksTilsyn: Boolean,
        val fordelingFaktor: BigDecimal,
        val skattefradragPerBarn: BigDecimal,
        val maksfradragAndel: BigDecimal,
        val skattefradrag: BigDecimal,
        val skattefradragMaksFradrag: BigDecimal,
        val skattefradragTotalTilsynsutgift: BigDecimal,
    )

    data class NotatTilsynsutgiftBarn(
        val gjelderBarn: DokumentmalPersonDto,
        val totalTilsynsutgift: BigDecimal,
        val faktiskUtgiftBeregnet: BigDecimal,
        val beløp: BigDecimal,
        val tilleggsstønadDagsats: BigDecimal? = null,
        val tilleggsstønadBeløp: BigDecimal? = null,
        val beløpstype: InntektBeløpstype? = null,
        val kostpenger: BigDecimal? = null,
        val tilleggsstønad: BigDecimal? = null,
    )

    data class NotatFaktiskTilsynsutgiftDto(
        val periode: DatoperiodeDto,
        val utgift: BigDecimal,
        val kostpenger: BigDecimal? = null,
        val kommentar: String? = null,
        val total: BigDecimal,
    )

    data class NotatStønadTilBarnetilsynDto(
        val periode: DatoperiodeDto,
        val skolealder: Skolealder,
        val tilsynstype: Tilsynstype,
        val kilde: Kilde = Kilde.MANUELL,
    ) {
        val skoleaderVisningsnavn = skolealder.visningsnavn.intern
        val tilsynstypeVisningsnavn = tilsynstype.visningsnavn.intern
    }

    data class NotatTilleggsstønadDto(
        val periode: DatoperiodeDto,
        val dagsats: BigDecimal?,
        val beløp: BigDecimal? = dagsats,
        val beløpstype: InntektBeløpstype = InntektBeløpstype.DAGSATS,
        val total: BigDecimal,
    ) {
        val beløpstypeVisningsnavn get() = beløpstype.tilVisningsnavn()
    }

    data class NotatUnderholdskostnadBeregningDto(
        val periode: DatoperiodeDto,
        val forbruk: BigDecimal = BigDecimal.ZERO,
        val boutgifter: BigDecimal = BigDecimal.ZERO,
        val stønadTilBarnetilsyn: BigDecimal = BigDecimal.ZERO,
        val tilsynsutgifter: BigDecimal = BigDecimal.ZERO,
        val barnetrygd: BigDecimal = BigDecimal.ZERO,
        val total: BigDecimal,
        val beregningsdetaljer: NotatUnderholdskostnadPeriodeBeregningsdetaljer? = null,
    )
}

data class NotatSamværDto(
    val erSammeForAlle: Boolean,
    val barn: List<NotatSamværBarnDto>,
)

data class NotatSamværBarnDto(
    val gjelderBarn: DokumentmalPersonDto,
    val begrunnelse: NotatBegrunnelseDto?,
    val perioder: List<NotatSamværsperiodeDto> = emptyList(),
) {
    data class NotatSamværsperiodeDto(
        val periode: DatoperiodeDto,
        val samværsklasse: Samværsklasse,
        val gjennomsnittligSamværPerMåned: BigDecimal,
        val beregning: SamværskalkulatorDetaljer? = null,
    ) {
        val samværsklasseVisningsnavn: String = samværsklasse.visningsnavn.intern

        val ferieVisningsnavnMap = SamværskalkulatorFerietype.entries.associateWith { it.visningsnavn.intern }
        val frekvensVisningsnavnMap = SamværskalkulatorNetterFrekvens.entries.associateWith { it.visningsnavn.intern }
    }
}

data class NotatSærbidragUtgifterDto(
    val beregning: NotatUtgiftBeregningDto? = null,
    val maksGodkjentBeløp: NotatMaksGodkjentBeløpDto? = null,
    val begrunnelse: NotatBegrunnelseDto,
    @Deprecated("Bruk begrunnelse", replaceWith = ReplaceWith("begrunnelse"))
    val notat: NotatBegrunnelseDto = begrunnelse,
    val utgifter: List<NotatUtgiftspostDto> = emptyList(),
    val totalBeregning: List<NotatTotalBeregningUtgifterDto> = emptyList(),
)

data class NotatTotalBeregningUtgifterDto(
    val betaltAvBp: Boolean,
    val utgiftstype: String,
    val totalKravbeløp: BigDecimal,
    val totalGodkjentBeløp: BigDecimal,
) {
    @get:Schema(name = "utgiftstypeVisningsnavn")
    val utgiftstypeVisningsnavn
        get() =
            try {
                Utgiftstype.valueOf(utgiftstype).visningsnavn.intern
            } catch (e: IllegalArgumentException) {
                utgiftstype
            }
}

data class NotatMaksGodkjentBeløpDto(
    val taMed: Boolean = true,
    val beløp: BigDecimal? = null,
    val begrunnelse: String? = null,
)

data class NotatUtgiftspostDto(
    @Schema(description = "Når utgifter gjelder. Kan være feks dato på kvittering")
    val dato: LocalDate,
    @Schema(
        description = "Type utgift. Kan feks være hva som ble kjøpt for kravbeløp (bugnad, klær, sko, etc)",
        oneOf = [Utgiftstype::class, String::class],
    )
    val type: String,
    @Schema(description = "Beløp som er betalt for utgiften det gjelder")
    val kravbeløp: BigDecimal,
    @Schema(description = "Beløp som er godkjent for beregningen")
    val godkjentBeløp: BigDecimal = kravbeløp,
    @Schema(description = "Begrunnelse for hvorfor godkjent beløp avviker fra kravbeløp. Må settes hvis godkjent beløp er ulik kravbeløp")
    val begrunnelse: String? = null,
    @Schema(description = "Om utgiften er betalt av BP")
    val betaltAvBp: Boolean = false,
) {
    @get:Schema(name = "utgiftstypeVisningsnavn")
    val utgiftstypeVisningsnavn
        get() =
            try {
                Utgiftstype.valueOf(type).visningsnavn.intern
            } catch (e: IllegalArgumentException) {
                type
            }
}

data class NotatUtgiftBeregningDto(
    @Schema(description = "Beløp som er direkte betalt av BP")
    val beløpDirekteBetaltAvBp: BigDecimal = BigDecimal.ZERO,
    @Schema(description = "Summen av godkjente beløp som brukes for beregningen")
    val totalGodkjentBeløp: BigDecimal = BigDecimal.ZERO,
    @Schema(description = "Summen av kravbeløp")
    val totalKravbeløp: BigDecimal = BigDecimal.ZERO,
    @Schema(description = "Summen av godkjente beløp som brukes for beregningen")
    val totalGodkjentBeløpBp: BigDecimal? = null,
    @Schema(description = "Summen av godkjent beløp for utgifter BP har betalt plus beløp som er direkte betalt av BP")
    val totalBeløpBetaltAvBp: BigDecimal = (totalGodkjentBeløpBp ?: BigDecimal.ZERO) + beløpDirekteBetaltAvBp,
)

data class NotatSærbidragKategoriDto(
    val kategori: Særbidragskategori,
    val beskrivelse: String? = null,
)

data class NotatVirkningstidspunktDto(
    @Schema(description = "Hvis det er likt for alle bruk avslag/årsak fra ett av barna")
    val erLikForAlle: Boolean,
    val erVirkningstidspunktLikForAlle: Boolean,
    val erAvslagForAlle: Boolean = false,
    val eldsteVirkningstidspunkt: YearMonth,
    val barn: List<NotatVirkningstidspunktBarnDto>,
)

data class NotatVirkningstidspunktBarnDto(
    val rolle: DokumentmalPersonDto,
    val stønadstype: Stønadstype?,
    val behandlingstype: Behandlingstype?,
    @Deprecated("Bruk behandlingstype")
    val søknadstype: String?,
    val vedtakstype: Vedtakstype?,
    val innkreving: Boolean = true,
    val søktAv: SøktAvType?,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate?,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM")
    val søktFraDato: YearMonth?,
    val beregnTilDato: YearMonth?,
    val opphørsdato: YearMonth?,
    val beregnTil: BeregnTil?,
    val etterfølgendeVedtakVirkningstidspunkt: YearMonth?,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val virkningstidspunkt: LocalDate?,
    val avslag: Resultatkode?,
    @get:Schema(name = "årsak", enumAsRef = true)
    @get:JsonProperty("årsak")
    val årsak: VirkningstidspunktÅrsakstype?,
    val begrunnelse: NotatBegrunnelseDto,
    val begrunnelseVurderingAvSkolegang: NotatBegrunnelseDto? = null,
    @Deprecated("Bruk begrunnelse", replaceWith = ReplaceWith("begrunnelse"))
    val notat: NotatBegrunnelseDto = begrunnelse,
) {
    @get:Schema(name = "behandlingstypeVisningsnavn")
    val behandlingstypeVisningsnavn get() = behandlingstype?.visningsnavn?.intern

    @get:Schema(name = "årsakVisningsnavn")
    @get:JsonProperty("årsakVisningsnavn")
    val årsakVisningsnavn get() = årsak?.visningsnavn?.intern

    @get:Schema(name = "avslagVisningsnavn")
    val avslagVisningsnavn
        get() =
            vedtakstype?.let {
                avslag?.visningsnavnIntern(
                    vedtakstype,
                    if (stønadstype == Stønadstype.FORSKUDD) {
                        rolle.harLøpendeForskudd == true
                    } else {
                        rolle.harLøpendeBidrag == true
                    },
                )
            }
                ?: avslag?.visningsnavn?.intern

    @get:Schema(name = "erAvvisning")
    val erAvvisning get() = avslag?.erAvvisning() == true

    @get:Schema(name = "avslagVisningsnavnUtenPrefiks")
    val avslagVisningsnavnUtenPrefiks
        get() = avslag?.visningsnavn?.intern
}

@Schema(description = "Notat begrunnelse skrevet av saksbehandler")
data class NotatBegrunnelseDto(
    val innhold: String?,
    val innholdFraOmgjortVedtak: String?,
    @get:Schema(name = "intern", deprecated = true)
    val intern: String? = innhold,
    val gjelder: DokumentmalPersonDto? = null,
)

data class NotatBoforholdDto(
    val barn: List<BoforholdBarn> = emptyList(),
    val andreVoksneIHusstanden: NotatAndreVoksneIHusstanden? = null,
    val boforholdBMSøknadsbarn: List<NotatBoforholdTilBMMedSøknadsbarn> = emptyList(),
    val sivilstand: NotatSivilstand,
    val begrunnelse: NotatBegrunnelseDto,
    @Deprecated("Bruk begrunnelse", replaceWith = ReplaceWith("begrunnelse"))
    val notat: NotatBegrunnelseDto = begrunnelse,
    val beregnetBoforhold: List<DelberegningBoforhold> = emptyList(),
)

data class NotatBoforholdTilBMMedSøknadsbarn(
    val gjelderBarn: DokumentmalPersonDto,
    val perioder: List<OpplysningerFraFolkeregisteret<Bostatuskode>> = emptyList(),
)

data class NotatGebyrV3Dto(
    val saker: List<NotatGebyrSakDto>,
)

data class NotatGebyrSakDto(
    val saksnummer: String,
    val gebyrRoller: List<NotatGebyrDetaljerDto>,
    val gebyr18År: List<NotatGebyrDetaljerDto>,
)

data class NotatGebyrV2Dto(
    val gebyrRoller: List<NotatGebyrRolleV2Dto>,
)

data class NotatGebyrRolleV3Dto(
    val gebyrDetaljer: NotatGebyrDetaljerDto,
    val rolle: DokumentmalPersonDto,
)

data class NotatGebyrRolleV2Dto(
    val gebyrDetaljer: List<NotatGebyrDetaljerDto>,
    val rolle: DokumentmalPersonDto,
)

data class NotatGebyrDetaljerDto(
    val søknad: NotatGebyrSøknadDetaljerDto? = null,
    val inntekt: NotatGebyrInntektDto,
    val manueltOverstyrtGebyr: NotatManueltOverstyrGebyrDto? = null,
    val beregnetIlagtGebyr: Boolean,
    val endeligIlagtGebyr: Boolean,
    val begrunnelse: String? = null,
    val beløpGebyrsats: BigDecimal,
    @Schema(deprecated = true)
    val rolle: DokumentmalPersonDto,
) {
    val erManueltOverstyrt get() = beregnetIlagtGebyr != endeligIlagtGebyr
    val gebyrResultatVisningsnavn get() =
        when (endeligIlagtGebyr) {
            true -> "Ilagt"
            false -> "Fritatt"
            else -> "Ikke valgt"
        }

    data class NotatGebyrSøknadDetaljerDto(
        val saksnummer: String,
        val søknadsid: Long,
        val mottattDato: LocalDate,
        var søknadFomDato: LocalDate? = null,
        val søktAvType: SøktAvType,
        val behandlingstype: Behandlingstype?,
        val behandlingstema: Behandlingstema?,
    ) {
        val søktAvTypeVisningsnavn get() = toVisningsnavn(søktAvType)
        val behandlingstypeVisningsnavn get() = behandlingstype?.visningsnavn?.intern
        val behandlingstemaVisningsnavn get() = toVisningsnavn(behandlingstema)
    }

    data class NotatGebyrInntektDto(
        val skattepliktigInntekt: BigDecimal,
        val maksBarnetillegg: BigDecimal? = null,
    ) {
        val totalInntekt get() = skattepliktigInntekt + (maksBarnetillegg ?: BigDecimal.ZERO)
    }

    data class NotatManueltOverstyrGebyrDto(
        val begrunnelse: String? = null,
        @Schema(description = "Skal bare settes hvis det er avslag")
        val ilagtGebyr: Boolean? = null,
    )
}

data class NotatSivilstand(
    val opplysningerFraFolkeregisteret: List<OpplysningerFraFolkeregisteret<SivilstandskodePDL>> =
        emptyList(),
    val opplysningerBruktTilBeregning: List<OpplysningerBruktTilBeregning<Sivilstandskode>> =
        emptyList(),
)

data class NotatAndreVoksneIHusstanden(
    val opplysningerFraFolkeregisteret:
        List<OpplysningerFraFolkeregisteretMedDetaljer<Bostatuskode, NotatAndreVoksneIHusstandenDetaljerDto>> =
        emptyList(),
    val opplysningerBruktTilBeregning: List<OpplysningerBruktTilBeregning<Bostatuskode>> =
        emptyList(),
)

data class NotatAndreVoksneIHusstandenDetaljerDto(
    val totalAntallHusstandsmedlemmer: Int,
    val husstandsmedlemmer: List<NotatVoksenIHusstandenDetaljerDto>,
)

data class NotatVoksenIHusstandenDetaljerDto(
    val navn: String,
    val fødselsdato: LocalDate?,
    val erBeskyttet: Boolean = false,
    val harRelasjonTilBp: Boolean,
)

data class BoforholdBarn(
    val gjelder: DokumentmalPersonDto,
    val medIBehandling: Boolean,
    val kilde: Kilde,
    val opplysningerFraFolkeregisteret: List<OpplysningerFraFolkeregisteret<Bostatuskode>> =
        emptyList(),
    val opplysningerBruktTilBeregning: List<OpplysningerBruktTilBeregning<Bostatuskode>> =
        emptyList(),
)
typealias OpplysningerFraFolkeregisteret<T> = OpplysningerFraFolkeregisteretMedDetaljer<T, Unit>

data class OpplysningerFraFolkeregisteretMedDetaljer<T, K : Any>(
    val periode: ÅrMånedsperiode,
    val status: T?,
    val detaljer: K? = null,
) {
    val statusVisningsnavn get() = toVisningsnavn(status)
}

data class OpplysningerBruktTilBeregning<T>(
    val periode: ÅrMånedsperiode,
    val status: T,
    val kilde: Kilde,
) {
    val statusVisningsnavn get() = toVisningsnavn(status)
}

private fun <T> toVisningsnavn(value: T): String? =
    when (val enum = value) {
        is Særbidragskategori -> {
            enum.visningsnavn.intern
        }

        is Utgiftstype -> {
            enum.visningsnavn.intern
        }

        is Bostatuskode -> {
            enum.visningsnavn.intern
        }

        is Inntektsrapportering -> {
            enum.visningsnavn.intern
        }

        is Resultatkode -> {
            enum.visningsnavn.intern
        }

        is Sivilstandskode -> {
            enum.visningsnavn.intern
        }

        is SivilstandskodePDL -> {
            enum.name.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        is Kilde -> {
            enum.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        is VirkningstidspunktÅrsakstype -> {
            enum.visningsnavn.intern
        }

        is SøktAvType -> {
            enum.name.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        is Behandlingstema -> {
            enum.name.lowercase().replace("_", " ").replace("pluss", "+").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        else -> {
            null
        }
    }

data class NotatInntekterDto(
    val inntekterPerRolle: List<InntekterPerRolle>,
    val offentligeInntekterPerRolle: List<InntekterPerRolle> = emptyList(),
    val notat: NotatBegrunnelseDto,
    val notatPerRolle: Set<NotatBegrunnelseDto> = emptySet(),
    val begrunnelsePerRolle: Set<NotatBegrunnelseDto> = notatPerRolle,
)

data class InntekterPerRolle(
    val gjelder: DokumentmalPersonDto,
    val arbeidsforhold: List<Arbeidsforhold> = emptyList(),
    @get:Schema(name = "årsinntekter")
    @get:JsonProperty("årsinntekter")
    val årsinntekter: List<NotatInntektDto> = emptyList(),
    val barnetillegg: List<NotatInntektDto> = emptyList(),
    val utvidetBarnetrygd: List<NotatInntektDto> = emptyList(),
    val småbarnstillegg: List<NotatInntektDto> = emptyList(),
    val kontantstøtte: List<NotatInntektDto> = emptyList(),
    val beregnetInntekter: List<NotatBeregnetInntektDto> = emptyList(),
) {
    val harInntekter get() =
        årsinntekter.isNotEmpty() ||
            barnetillegg.isNotEmpty() ||
            utvidetBarnetrygd.isNotEmpty() ||
            småbarnstillegg.isNotEmpty() ||
            kontantstøtte.isNotEmpty()
}

data class NotatBeregnetInntektDto(
    val gjelderBarn: DokumentmalPersonDto,
    val summertInntektListe: List<DelberegningSumInntekt>,
)

data class Arbeidsforhold(
    val periode: ÅrMånedsperiode,
    val arbeidsgiver: String,
    val stillingProsent: String?,
    val lønnsendringDato: LocalDate?,
)

data class NotatInntektDto(
    val periode: ÅrMånedsperiode?,
    val opprinneligPeriode: ÅrMånedsperiode?,
    val beløp: BigDecimal,
    val kilde: Kilde = Kilde.OFFENTLIG,
    val type: Inntektsrapportering,
    val medIBeregning: Boolean = false,
    val gjelderBarn: DokumentmalPersonDto?,
    val historisk: Boolean = false,
    val inntektsposter: List<NotatInntektspostDto> = emptyList(),
) {
    val beløpstypeVisningsnavn get() = beløpstype.tilVisningsnavn()

    val skattefaktor get() =
        if (type == Inntektsrapportering.BARNETILLEGG) {
            inntektsposter.firstOrNull()?.skattefaktor ?: BigDecimal.ZERO
        } else {
            null
        }
    val beløpstype get() =
        if (type == Inntektsrapportering.BARNETILLEGG) {
            if (inntektsposter.firstOrNull()?.beløpstype == null ||
                inntektsposter.firstOrNull()?.beløpstype == InntektBeløpstype.ÅRSBELØP
            ) {
                InntektBeløpstype.MÅNEDSBELØP
            } else {
                inntektsposter.firstOrNull()?.beløpstype
            }
        } else {
            InntektBeløpstype.ÅRSBELØP
        }

    @get:Schema(description = "Avrundet månedsbeløp for barnetillegg")
    val beløpMånedDagsats: BigDecimal?
        get() =
            when (beløpstype) {
                InntektBeløpstype.MÅNEDSBELØP -> månedsbeløp
                InntektBeløpstype.DAGSATS -> dagsats
                else -> null
            }

    @get:Schema(description = "Avrundet månedsbeløp for barnetillegg")
    val månedsbeløp: BigDecimal?
        get() =
            run {
                val beløpstype = inntektsposter.firstOrNull()?.beløpstype
                if (Inntektsrapportering.BARNETILLEGG == type && beløpstype == InntektBeløpstype.MÅNEDSBELØP) {
                    inntektsposter.first().beløp
                } else if (Inntektsrapportering.BARNETILLEGG == type &&
                    (beløpstype == null || beløpstype == InntektBeløpstype.ÅRSBELØP)
                ) {
                    beløp.divide(BigDecimal(12), 0, RoundingMode.HALF_UP)
                } else {
                    null
                }
            }

    @get:Schema(description = "Avrundet dagsats for barnetillegg")
    val dagsats: BigDecimal?
        get() =
            if (Inntektsrapportering.BARNETILLEGG == type && beløpstype == InntektBeløpstype.DAGSATS) {
                inntektsposter.first().beløp
            } else {
                null
            }

    val visningsnavn
        get() =
            type.visningsnavnMedÅrstall(
                opprinneligPeriode?.fom?.year ?: periode?.fom?.year,
                opprinneligPeriode,
            )
}

data class NotatInntektspostDto(
    val kode: String?,
    val inntektstype: Inntektstype?,
    val beløp: BigDecimal,
    val beløpstype: InntektBeløpstype = InntektBeløpstype.ÅRSBELØP,
    val skattefaktor: BigDecimal? = null,
    val visningsnavn: String? = inntektstype?.visningsnavn?.intern,
)

data class NotatVedtakDetaljerDto(
    val erFattet: Boolean,
    val fattetAvSaksbehandler: String?,
    val fattetTidspunkt: LocalDateTime?,
    val kanFatteVedtakForRevurderingsbarn: Boolean = false,
    val skalFatteVedtakForRevurderingsbarn: Boolean = false,
    val manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse: String? = null,
    val resultat: List<VedtakResultatInnhold>,
)

data class PeriodeSlåttUtTilFF(
    val periode: ÅrMånedsperiode,
    val erEvneJustertNedTil25ProsentAvInntekt: Boolean,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = NotatResultatSærbidragsberegningDto::class, name = "SÆRBIDRAG"),
    JsonSubTypes.Type(value = NotatResultatForskuddBeregningBarnDto::class, name = "FORSKUDD"),
    JsonSubTypes.Type(value = DokumentmalResultatBidragsberegningBarnDto::class, name = "BIDRAG"),
)
abstract class VedtakResultatInnhold(
    val type: NotatMalType,
)

data class NotatResultatSærbidragsberegningDto(
    val periode: ÅrMånedsperiode,
    val bpsAndel: DelberegningBidragspliktigesAndel? = null,
    val beregning: UtgiftBeregningDto? = null,
    val forskuddssats: BigDecimal? = null,
    val maksGodkjentBeløp: BigDecimal? = null,
    val inntekter: DokumentmalResultatBeregningInntekterDto? = null,
    val delberegningBidragspliktigesBeregnedeTotalbidrag: DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto? = null,
    val delberegningBidragsevne: DokumentmalDelberegningBidragsevneDto? = null,
    val delberegningUtgift: DelberegningUtgift? = null,
    val resultat: BigDecimal,
    val resultatKode: Resultatkode,
    val antallBarnIHusstanden: Double? = null,
    val voksenIHusstanden: Boolean? = null,
    val enesteVoksenIHusstandenErEgetBarn: Boolean? = null,
    val erDirekteAvslag: Boolean = false,
    val bpHarEvne: Boolean = resultatKode != Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE,
) : VedtakResultatInnhold(NotatMalType.SÆRBIDRAG) {
    val resultatVisningsnavn get() = resultatKode.visningsnavn.intern
    val beløpSomInnkreves: BigDecimal
        get() =
            maxOf(
                resultat - (beregning?.totalBeløpBetaltAvBp ?: BigDecimal.ZERO),
                BigDecimal.ZERO,
            )

    data class UtgiftBeregningDto(
        @Schema(description = "Beløp som er direkte betalt av BP")
        val beløpDirekteBetaltAvBp: BigDecimal = BigDecimal.ZERO,
        @Schema(description = "Summen av godkjente beløp som brukes for beregningen")
        val totalGodkjentBeløp: BigDecimal = BigDecimal.ZERO,
        @Schema(description = "Summen av kravbeløp")
        val totalKravbeløp: BigDecimal = BigDecimal.ZERO,
        @Schema(description = "Summen av godkjente beløp som brukes for beregningen")
        val totalGodkjentBeløpBp: BigDecimal? = null,
        @Schema(description = "Summen av godkjent beløp for utgifter BP har betalt plus beløp som er direkte betalt av BP")
        val totalBeløpBetaltAvBp: BigDecimal = (totalGodkjentBeløpBp ?: BigDecimal.ZERO) + beløpDirekteBetaltAvBp,
    )
}

data class NotatResultatForskuddBeregningBarnDto(
    val barn: DokumentmalPersonDto,
    val perioder: List<NotatResultatPeriodeDto>,
) : VedtakResultatInnhold(NotatMalType.FORSKUDD) {
    data class NotatResultatPeriodeDto(
        val periode: ÅrMånedsperiode,
        val beløp: BigDecimal,
        val resultatKode: Resultatkode,
        val regel: String,
        val sivilstand: Sivilstandskode?,
        val inntekt: BigDecimal,
        val vedtakstype: Vedtakstype?,
        val antallBarnIHusstanden: Int,
    ) {
        val resultatKodeVisningsnavn
            get() = vedtakstype?.let { resultatKode.visningsnavnIntern(it) } ?: resultatKode.visningsnavn.intern
        val sivilstandVisningsnavn get() = sivilstand?.visningsnavn?.intern
    }
}

data class EndeligOrkestrertVedtak(
    val type: Vedtakstype?,
    val perioder: List<DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto>,
)

data class NotatPrivatAvtaleDto(
    val gjelderBarn: DokumentmalPersonDto,
    val avtaleDato: LocalDate?,
    val avtaleType: PrivatAvtaleType?,
    val skalIndeksreguleres: Boolean,
    val utlandsbidrag: Boolean = false,
    val begrunnelse: NotatBegrunnelseDto? = null,
    val perioder: List<NotatPrivatAvtalePeriodeDto> = emptyList(),
    val vedtakslisteUtenInnkreving: List<DokumentmalManuellVedtak> = emptyList(),
    val beregnetPrivatAvtalePerioder: List<NotatBeregnetPrivatAvtalePeriodeDto> = emptyList(),
) {
    val avtaleTypeVisningsnavn get() = avtaleType?.visningsnavn?.intern
}

data class NotatPrivatAvtalePeriodeDto(
    val periode: DatoperiodeDto,
    val beløp: BigDecimal,
    val samværsklasse: Samværsklasse? = null,
    val valutakode: Valutakode? = null,
)

data class NotatBeregnetPrivatAvtalePeriodeDto(
    val periode: DatoperiodeDto,
    val indeksfaktor: BigDecimal,
    val beløp: BigDecimal,
)

typealias NotatResultatForskuddBeregningBarnListe = List<NotatResultatForskuddBeregningBarnDto>
