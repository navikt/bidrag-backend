package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagDatakilde
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.time.LocalDate
import java.time.LocalDateTime

data class InnhentetSivilstand(
    override val datakilde: GrunnlagDatakilde = GrunnlagDatakilde.PDL,
    override val grunnlag: List<SivilstandPDL>,
    override val hentetTidspunkt: LocalDateTime,
) : InnhentetGrunnlagInnhold<List<InnhentetSivilstand.SivilstandPDL>> {
    data class SivilstandPDL(
        val gyldigFom: LocalDate?,
        @Schema(description = "Type sivilstand fra PDL")
        val sivilstand: SivilstandskodePDL?,
        @Schema(description = "Dato NAV tidligst har fått bekreftet sivilstanden")
        val bekreftelsesdato: LocalDate?,
        @Schema(description = "Master for opplysningen om sivilstand (FREG eller PDL)")
        val master: String?,
        @Schema(description = "Angir om sivilstanden er historisk (true) eller aktiv (false)")
        val historisk: Boolean?,
        @Schema(description = "Tidspunktet sivilstanden er registrert")
        val registrert: LocalDateTime?,
    )
}

@Schema(description = "Sivilstand for person")
data class SivilstandPeriode(
    override val periode: ÅrMånedsperiode,
    val sivilstand: Sivilstandskode,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold
