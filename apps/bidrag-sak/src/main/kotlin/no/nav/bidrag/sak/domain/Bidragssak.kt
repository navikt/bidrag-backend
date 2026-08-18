package no.nav.bidrag.sak.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.mapper.RolleMapper.toRolleDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.BidragssakPipDto
import no.nav.bidrag.transport.sak.OppdaterSakResponse
import java.time.LocalDate
import java.util.Objects

@Entity(name = "T_BIDRAG_SAK")
open class Bidragssak(
    @Id
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "EIER_FOGD", columnDefinition = "CHAR(4)")
    open var eierfogd: String,
    open var fogdFomDato: LocalDate = LocalDate.now(),
    @Column(name = "BIDR_STAT_KODE")
    @Enumerated(EnumType.STRING)
    open var status: Bidragssakstatus = Bidragssakstatus.NY,
    open var opprettetDato: LocalDate = LocalDate.now(),
    @Column(name = "KATEGORI_KODE")
    @Convert(converter = SakskategoriConverter::class)
    open var kategori: Sakskategori = Sakskategori.NASJONAL,
    @Column(name = "PART_ER_ANSATT")
    @Convert(converter = BooleanConverter::class)
    open var ansatt: Boolean = false,
    @Column(name = "INHABILITET_I_SAK")
    @Convert(converter = BooleanConverter::class)
    open var inhabilitet: Boolean = false,
    @Column(name = "LEVDE_ADSKILT")
    @Convert(converter = BooleanConverter::class)
    open var levdeAdskilt: Boolean = false,
    open var sanertDato: LocalDate? = null,
    @Column(name = "KONV_KODE")
    @Enumerated(EnumType.STRING)
    open var konvensjon: Konvensjon? = null,
    @Column(name = "KONV_DATO")
    open var konvensjonsdato: LocalDate? = null,
    @Column(name = "FFU_REF_NR")
    open var ffuReferansenr: String? = null,
    @Column(name = "LAND")
    open var land: String? = null,
    @Column(name = "VEDTAKSPERRE")
    open var vedtakSperre: String? = null,
    @Column(name = "AVSLUTTET_TIDSPUNKT")
    open var avsluttetTidspunkt: LocalDate? = null,
    @Column(name = "ARBFOR_KODE")
    @Convert(converter = ArbeidsfordelingConverter::class)
    open var arbeidsfordeling: Arbeidsfordeling = Arbeidsfordeling.EIERENHET,
    @OneToMany(mappedBy = "bidragssak", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    open var roller: MutableSet<Rolle> = mutableSetOf(),
    @OneToMany(mappedBy = "bidragssak", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    open var tilganger: MutableSet<Tilgang> = mutableSetOf(),
) {
    fun erAvsluttet() = avsluttetTidspunkt != null && avsluttetTidspunkt!! <= LocalDate.now()

    val primærrolle
        get() =
            this.roller.firstOrNull { it.rolleType == Rolletype.BIDRAGSMOTTAKER }
                ?: this.roller.firstOrNull { it.rolleType == Rolletype.BARN }
                ?: this.roller.firstOrNull { it.rolleType == Rolletype.BIDRAGSPLIKTIG }
                ?: error("Sak med saksnummer $saksnummer har ingen roller.")

    fun tilBidragSakDto(
        begrensetTilgang: Boolean,
        fødselsnummer: Personident? = null,
        visRollehistorikk: Boolean = false,
    ): BidragssakDto {
        val filtrerteRoller = if (begrensetTilgang) roller.filter { it.fødselsnummer == fødselsnummer?.verdi } else roller

        return BidragssakDto(
            eierfogd = Enhetsnummer(eierfogd),
            saksnummer = Saksnummer(saksnummer),
            saksstatus = status,
            kategori = kategori,
            begrensetTilgang = begrensetTilgang,
            opprettetDato = opprettetDato,
            levdeAdskilt = levdeAdskilt,
            ukjentPart = roller.any { it.ukjentPart != null },
            vedtakssperre = vedtakSperre != null,
            avsluttet = erAvsluttet(),
            roller = filtrerteRoller.toRolleDto(visRollehistorikk),
            arbeidsfordeling = arbeidsfordeling,
        )
    }

    fun tilBidragSakPipDto(): BidragssakPipDto = BidragssakPipDto(
        saksnummer = Saksnummer(saksnummer),
        avsluttet = erAvsluttet(),
        roller = roller.filter { it.erPerson() }.mapNotNull { it.fødselsnummer }.distinct(),
    )

    fun tilOppdaterSakResponse(): OppdaterSakResponse = OppdaterSakResponse(
        saksnummer = Saksnummer(saksnummer),
        eierfogd = Enhetsnummer(eierfogd),
        kategorikode = kategori,
        status = status,
        ansatt = ansatt,
        inhabilitet = inhabilitet,
        levdeAdskilt = levdeAdskilt,
        sanertDato = sanertDato,
        arbeidsfordeling = arbeidsfordeling,
        landkode = land?.let { Landkode(it) },
        konvensjonskode = konvensjon,
        konvensjonsdato = konvensjonsdato,
        ffuReferansenr = ffuReferansenr,
        roller = roller.toRolleDto(true),
    )

    override fun hashCode(): Int = Objects.hash(
        saksnummer,
        eierfogd,
        fogdFomDato,
        status,
        opprettetDato,
        kategori,
        ansatt,
        inhabilitet,
        levdeAdskilt,
        sanertDato,
        konvensjon,
        konvensjonsdato,
        ffuReferansenr,
        land,
        arbeidsfordeling,
        roller,
        tilganger,
    )

    override fun toString(): String = "Bidragssak(saksnummer='$saksnummer', eierfogd='$eierfogd', fogdFomDato=$fogdFomDato, status=$status," +
        " opprettetDato=$opprettetDato, kategori=$kategori, ansatt=$ansatt, inhabilitet=$inhabilitet, " +
        "levdeAdskilt=$levdeAdskilt, sanertDato=$sanertDato, konvensjon=$konvensjon," +
        " konvensjonsdato=$konvensjonsdato, ffuReferansenr=$ffuReferansenr, land=$land, " +
        "arbeidsfordeling=$arbeidsfordeling, roller=$roller, tilganger=$tilganger, " +
        "avsluttetTidspunkt=$avsluttetTidspunkt, vedtakSperre=$vedtakSperre "

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bidragssak

        if (saksnummer != other.saksnummer) return false
        if (eierfogd != other.eierfogd) return false
        if (fogdFomDato != other.fogdFomDato) return false
        if (status != other.status) return false
        if (opprettetDato != other.opprettetDato) return false
        if (kategori != other.kategori) return false
        if (ansatt != other.ansatt) return false
        if (inhabilitet != other.inhabilitet) return false
        if (levdeAdskilt != other.levdeAdskilt) return false
        if (sanertDato != other.sanertDato) return false
        if (konvensjon != other.konvensjon) return false
        if (konvensjonsdato != other.konvensjonsdato) return false
        if (ffuReferansenr != other.ffuReferansenr) return false
        if (land != other.land) return false
        if (arbeidsfordeling != other.arbeidsfordeling) return false
        if (roller != other.roller) return false
        if (avsluttetTidspunkt != other.avsluttetTidspunkt) return false
        if (vedtakSperre != other.vedtakSperre) return false
        if (tilganger != other.tilganger) return false

        return true
    }
}
