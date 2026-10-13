package no.nav.bidrag.sak.mapper

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RolleMapperTest {
    @Nested
    inner class ToRolleDto {
        @Test
        fun `barn med REELMOTTAKER=SAMHANDLER eksponeres i DTO (både deprecated og ny DTO)`() {
            val barn =
                Rolle(rolleType = Rolletype.BARN).apply {
                    rolleId = 10
                    fødselsnummer = "06510985255"
                }
            val rm =
                Rolle(rolleType = Rolletype.REELMOTTAKER).apply {
                    rolleId = 99
                    samhandlerIdent = "85000000083"
                }
            barn.rmRolleId = rm.rolleId

            val dtoer = RolleMapper.run { setOf(barn, rm).toRolleDto(true) }
            val barnDto = dtoer.first { it.type == Rolletype.BARN }

            // Deprecated felt
            assertThat(barnDto.reellMottager).isNotNull
            assertThat(barnDto.reellMottager!!.verdi).isEqualTo("85000000083")

            // Ny DTO
            assertThat(barnDto.reellMottaker).isNotNull
            assertThat(barnDto.reellMottaker!!.ident).isEqualTo(ReellMottaker("85000000083"))
        }

        @Test
        fun `barn med REELMOTTAKER=PERSON eksponeres i DTO (både deprecated og ny DTO)`() {
            val barn =
                Rolle(rolleType = Rolletype.BARN).apply {
                    rolleId = 10
                    fødselsnummer = "06510985255"
                }
            val rm =
                Rolle(rolleType = Rolletype.REELMOTTAKER).apply {
                    rolleId = 99
                    fødselsnummer = "02451550643"
                    mottagerErVerge = true
                }
            barn.rmRolleId = rm.rolleId

            val dtoer = RolleMapper.run { setOf(barn, rm).toRolleDto(true) }
            val barnDto = dtoer.first { it.type == Rolletype.BARN }

            // Deprecated
            assertThat(barnDto.reellMottager).isNotNull
            assertThat(barnDto.reellMottager!!.verdi).isEqualTo("02451550643")

            // Ny DTO (inkl. verge-flagget)
            assertThat(barnDto.reellMottaker).isNotNull
            assertThat(barnDto.reellMottaker!!.ident).isEqualTo(ReellMottaker("02451550643"))
            assertThat(barnDto.reellMottaker!!.verge).isTrue()
        }

        @Test
        fun `barn uten REELMOTTAKER eksponerer ikke RM-feltene`() {
            val barn =
                Rolle(rolleType = Rolletype.BARN).apply {
                    rolleId = 10
                    fødselsnummer = "06510985255"
                }

            val dtoer = RolleMapper.run { setOf(barn).toRolleDto(true) }
            val barnDto = dtoer.first { it.type == Rolletype.BARN }

            assertThat(barnDto.reellMottager).isNull()
            assertThat(barnDto.reellMottaker).isNull()
        }
    }

    @Nested
    inner class HarRM {
        @Test
        fun `harRM er true når reellMottaker har personident (ny DTO)`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottaker = ReellMottakerDto(ident = ReellMottaker("02451550643"), verge = false),
                )
            assertThat(dto.harRM()).isTrue()
        }

        @Test
        fun `harRM er true når reellMottaker har samhandlerId (ny DTO)`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottaker = ReellMottakerDto(ident = ReellMottaker("85000000083"), verge = false),
                )
            assertThat(dto.harRM()).isTrue()
        }

        @Test
        fun `harRM er true når reellMottager (DEPRECATED) har personident`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottager = ReellMottaker("02451550643"),
                )
            assertThat(dto.harRM()).isTrue()
        }

        @Test
        fun `harRM er true når reellMottager (DEPRECATED) har samhandlerId`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottager = ReellMottaker("85000000083"),
                )
            assertThat(dto.harRM()).isTrue()
        }

        @Test
        fun `harRM er false når ingen RM-felt er satt`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                )
            assertThat(dto.harRM()).isFalse()
        }

        @Test
        fun `harRM er false når RM er satt men ident er tomt eller ugyldig`() {
            val dtoNy =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottaker = ReellMottakerDto(ident = ReellMottaker(""), verge = false),
                )
            assertThat(dtoNy.harRM()).isFalse()

            val dtoDep =
                RolleDto(
                    fødselsnummer = Personident("06510985255"),
                    type = Rolletype.BARN,
                    reellMottager = ReellMottaker(""),
                )
            assertThat(dtoDep.harRM()).isFalse()
        }

        @Test
        fun `harRM er false for BIDRAGSPLIKTIG uten reell mottaker`() {
            val dto =
                RolleDto(
                    fødselsnummer = Personident("62487144350"),
                    type = Rolletype.BIDRAGSPLIKTIG,
                )
            assertThat(dto.harRM()).isFalse()
        }
    }
}
