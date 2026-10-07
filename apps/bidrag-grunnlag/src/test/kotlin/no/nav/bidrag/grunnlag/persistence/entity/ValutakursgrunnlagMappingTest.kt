package no.nav.bidrag.grunnlag.persistence.entity

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.bo.toValutakursgrunnlagEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDateTime

class ValutakursgrunnlagMappingTest {
    private val brukFra = LocalDateTime.of(2026, 1, 1, 0, 0)
    private val brukTil = LocalDateTime.of(2026, 7, 1, 0, 0)

    @Test
    fun `sluttidspunkt er ikke nullable og har seks måneders standardperiode`() {
        val bo = ValutakursgrunnlagBo(brukFra = brukFra, kurs = null, multiplikator = null, basisvaluta = Valutakode.EUR)
        val entity = Valutakursgrunnlag(brukFra = brukFra, basisvaluta = Valutakode.EUR)

        assertEquals(brukTil, bo.brukTil)
        assertEquals(brukTil, entity.brukTil)
        assertEquals(brukTil, bo.toValutakursgrunnlagEntity().brukTil)
        assertEquals(brukTil, entity.toValutakursgrunnlagBo().brukTil)
        assertEquals(false, ValutakursgrunnlagBo::class.constructors.single().parameters.single { it.name == "brukTil" }.type.isMarkedNullable)
        assertEquals(false, Valutakursgrunnlag::class.constructors.single().parameters.single { it.name == "brukTil" }.type.isMarkedNullable)
    }

    @Test
    fun `feilet kurs kan mappes frem og tilbake uten kurs`() {
        val bo = ValutakursgrunnlagBo(
            brukFra = brukFra,
            brukTil = brukTil,
            kurs = null,
            multiplikator = null,
            basisvaluta = Valutakode.EUR,
            feiletHenting = true,
        )

        val entity = bo.toValutakursgrunnlagEntity()

        assertEquals(ValutakursgrunnlagStatus.FEILET, entity.status)
        assertNull(entity.kurs)
        Mockito.mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS).use { tid ->
            tid.`when`<LocalDateTime> { LocalDateTime.now() }.thenReturn(brukTil)

            assertEquals(bo.copy(aktiv = false, oppdatertTidspunkt = entity.oppdatertTidspunkt), entity.toValutakursgrunnlagBo())
        }
    }

    @Test
    fun `hentet og overstyrt kurs kan leses tilbake til bo`() {
        val bo = ValutakursgrunnlagBo(
            brukFra = brukFra,
            brukTil = brukTil,
            kurs = BigDecimal("12.1234567890123456"),
            multiplikator = 0,
            basisvaluta = Valutakode.EUR,
        )
        val entity = bo.toValutakursgrunnlagEntity()

        assertEquals(ValutakursgrunnlagStatus.HENTET, entity.status)
        entity.status = ValutakursgrunnlagStatus.OVERSTYRT
        entity.oppdatertTidspunkt = LocalDateTime.of(2026, 2, 1, 12, 0)

        Mockito.mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS).use { tid ->
            tid.`when`<LocalDateTime> { LocalDateTime.now() }.thenReturn(brukTil)

            assertEquals(
                bo.copy(aktiv = false, status = ValutakursgrunnlagStatus.OVERSTYRT, oppdatertTidspunkt = entity.oppdatertTidspunkt),
                entity.toValutakursgrunnlagBo(),
            )
        }
        assertEquals(ValutakursgrunnlagStatus.OVERSTYRT, entity.status)
    }
}
