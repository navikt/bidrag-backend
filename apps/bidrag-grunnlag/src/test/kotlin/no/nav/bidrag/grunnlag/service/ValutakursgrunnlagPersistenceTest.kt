package no.nav.bidrag.grunnlag.service

import jakarta.persistence.EntityManager
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.bo.toValutakursgrunnlagEntity
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagStatus
import no.nav.bidrag.grunnlag.persistence.repository.AinntektRepository
import no.nav.bidrag.grunnlag.persistence.repository.AinntektspostRepository
import no.nav.bidrag.grunnlag.persistence.repository.BarnetilleggRepository
import no.nav.bidrag.grunnlag.persistence.repository.BarnetilsynRepository
import no.nav.bidrag.grunnlag.persistence.repository.GrunnlagspakkeRepository
import no.nav.bidrag.grunnlag.persistence.repository.KontantstotteRepository
import no.nav.bidrag.grunnlag.persistence.repository.RelatertPersonRepository
import no.nav.bidrag.grunnlag.persistence.repository.SivilstandRepository
import no.nav.bidrag.grunnlag.persistence.repository.SkattegrunnlagRepository
import no.nav.bidrag.grunnlag.persistence.repository.SkattegrunnlagspostRepository
import no.nav.bidrag.grunnlag.persistence.repository.UtvidetBarnetrygdOgSmaabarnstilleggRepository
import no.nav.bidrag.grunnlag.persistence.repository.ValutakursgrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class ValutakursgrunnlagPersistenceTest {
    private val repository = Mockito.mock(ValutakursgrunnlagRepository::class.java)
    private val service = PersistenceService(
        Mockito.mock(GrunnlagspakkeRepository::class.java),
        Mockito.mock(AinntektRepository::class.java),
        Mockito.mock(AinntektspostRepository::class.java),
        Mockito.mock(SkattegrunnlagRepository::class.java),
        Mockito.mock(SkattegrunnlagspostRepository::class.java),
        Mockito.mock(UtvidetBarnetrygdOgSmaabarnstilleggRepository::class.java),
        Mockito.mock(BarnetilleggRepository::class.java),
        Mockito.mock(RelatertPersonRepository::class.java),
        Mockito.mock(SivilstandRepository::class.java),
        Mockito.mock(KontantstotteRepository::class.java),
        Mockito.mock(BarnetilsynRepository::class.java),
        repository,
        Mockito.mock(EntityManager::class.java),
    )
    private val brukFra = LocalDateTime.of(2026, 7, 1, 0, 0)
    private val grunnlag = ValutakursgrunnlagBo(
        brukFra = brukFra,
        brukTil = brukFra.plusMonths(6),
        kurs = BigDecimal("10.5"),
        multiplikator = 0,
        basisvaluta = Valutakode.USD,
    )

    @Test
    fun `gjenforsøk oppdaterer eksisterende rad i stedet for å opprette ny`() {
        val eksisterende = Valutakursgrunnlag(
            valutakursgrunnlagId = 42,
            brukFra = brukFra,
            brukTil = brukFra.plusMonths(6),
            basisvaluta = Valutakode.USD,
            feiletHenting = true,
        )
        Mockito.`when`(repository.findByBasisvalutaAndBrukFra(Valutakode.USD, brukFra)).thenReturn(eksisterende)
        Mockito.`when`(repository.saveAndFlush(Mockito.any(Valutakursgrunnlag::class.java))).thenAnswer { it.getArgument(0) }

        val resultat = service.opprettValutakursgrunnlag(grunnlag)

        assertEquals(42, resultat.valutakursgrunnlagId)
        assertEquals(ValutakursgrunnlagStatus.HENTET, resultat.status)
        assertEquals(BigDecimal("10.5"), resultat.kurs)
    }

    @Test
    fun `gjenforsøk bevarer overstyrt kurs`() {
        val eksisterende = Valutakursgrunnlag(
            valutakursgrunnlagId = 42,
            brukFra = brukFra,
            brukTil = brukFra.plusMonths(6),
            kurs = BigDecimal("12"),
            basisvaluta = Valutakode.USD,
        ).apply { status = ValutakursgrunnlagStatus.OVERSTYRT }
        Mockito.`when`(repository.findByBasisvalutaAndBrukFra(Valutakode.USD, brukFra)).thenReturn(eksisterende)

        assertSame(eksisterende, service.opprettValutakursgrunnlag(grunnlag))
        Mockito.verify(repository, Mockito.never()).saveAndFlush(Mockito.any(Valutakursgrunnlag::class.java))
    }

    @Test
    fun `overstyring erstatter feilet kurs på samme rad`() {
        val eksisterende = Valutakursgrunnlag(
            valutakursgrunnlagId = 42,
            brukFra = brukFra,
            brukTil = brukFra.plusMonths(6),
            basisvaluta = Valutakode.USD,
            feiletHenting = true,
        )
        Mockito.`when`(repository.findById(42)).thenReturn(Optional.of(eksisterende))
        Mockito.`when`(repository.saveAndFlush(Mockito.any(Valutakursgrunnlag::class.java))).thenAnswer { it.getArgument(0) }

        val overstyrt = service.overstyrValutakursgrunnlag(42, BigDecimal("10.2500"))

        assertEquals(42, overstyrt.valutakursgrunnlagId)
        assertEquals(ValutakursgrunnlagStatus.OVERSTYRT, overstyrt.status)
        assertEquals(BigDecimal("10.2500"), overstyrt.kurs)
        assertEquals(false, overstyrt.feiletHenting)
        assertEquals(ValutakursgrunnlagKilde.MANUELL, overstyrt.kilde)
    }

    @Test
    fun `overstyring avviser nullkurs`() {
        assertEquals(
            HttpStatus.BAD_REQUEST,
            assertThrows<ResponseStatusException> { service.overstyrValutakursgrunnlag(42, BigDecimal.ZERO) }.statusCode,
        )
        Mockito.verifyNoInteractions(repository)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "10000000000000000000000",
            "99999999999999999999999.1234567890123456",
            "1E+22",
            "1E+2147483647",
            "0.00000000000000001",
            "10.12345678901234567",
            "9999999999999999999999.99999999999999999",
            "-1",
        ],
    )
    fun `overstyring avviser kurser som ikke kan lagres eksakt før databaseoppslag`(verdi: String) {
        val feil = assertThrows<ResponseStatusException> { service.overstyrValutakursgrunnlag(42, BigDecimal(verdi)) }

        assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
        Mockito.verifyNoInteractions(repository, service.entityManager)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "9999999999999999999999.9999999999999999",
            "9999999999999999999999",
            "0.0000000000000001",
            "1E+21",
            "1E-16",
            "10.1234567890123456000",
            "9999999999999999999999.00000000000000000",
            "0.0000000000000001000",
        ],
    )
    fun `overstyring lagrer representerbare grenseverdier uten avrunding`(verdi: String) {
        val kurs = BigDecimal(verdi)
        val eksisterende = grunnlag.toValutakursgrunnlagEntity()
        Mockito.`when`(repository.findById(42)).thenReturn(Optional.of(eksisterende))
        Mockito.`when`(repository.saveAndFlush(Mockito.any(Valutakursgrunnlag::class.java))).thenAnswer { it.getArgument(0) }

        val resultat = service.overstyrValutakursgrunnlag(42, kurs)

        assertEquals(kurs, resultat.kurs)
        assertEquals(ValutakursgrunnlagStatus.OVERSTYRT, resultat.status)
    }
}
