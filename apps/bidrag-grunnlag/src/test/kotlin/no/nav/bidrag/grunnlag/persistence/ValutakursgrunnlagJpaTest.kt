package no.nav.bidrag.grunnlag.persistence

import jakarta.persistence.EntityManager
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.BidragGrunnlag
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagStatus
import no.nav.bidrag.grunnlag.persistence.repository.ValutakursgrunnlagRepository
import no.nav.bidrag.grunnlag.service.PersistenceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@Import(PersistenceService::class)
@ContextConfiguration(classes = [BidragGrunnlag::class])
@TestPropertySource(properties = ["spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"])
class ValutakursgrunnlagJpaTest(
    @Autowired private val repository: ValutakursgrunnlagRepository,
    @Autowired private val entityManager: EntityManager,
    @Autowired private val persistenceService: PersistenceService,
) {
    @Test
    fun `gjenforsøk og overstyring bruker samme rad og bevarer manuell kurs`() {
        val dato = LocalDate.of(2024, 1, 1)
        val grunnlag = ValutakursgrunnlagBo(
            brukFra = dato.atStartOfDay(),
            brukTil = dato.plusMonths(6).atStartOfDay(),
            kurs = null,
            multiplikator = null,
            basisvaluta = Valutakode.USD,
            feiletHenting = true,
        )
        val lagret = persistenceService.opprettValutakursgrunnlag(grunnlag)
        val hentet = persistenceService.opprettValutakursgrunnlag(
            grunnlag.copy(
                kurs = BigDecimal("10.125"),
                multiplikator = 0,
                feiletHenting = false,
                status = ValutakursgrunnlagStatus.HENTET,
                kilde = ValutakursgrunnlagKilde.ECB,
                observasjonsdato = LocalDate.of(2023, 12, 1),
            ),
        )
        assertEquals(lagret.valutakursgrunnlagId, hentet.valutakursgrunnlagId)
        assertEquals(ValutakursgrunnlagStatus.HENTET, hentet.status)
        assertEquals(ValutakursgrunnlagKilde.ECB, hentet.kilde)
        assertEquals(LocalDate.of(2023, 12, 1), hentet.observasjonsdato)

        val overstyrt = persistenceService.overstyrValutakursgrunnlag(lagret.valutakursgrunnlagId, BigDecimal("11"))
        val etterGjenforsøk = persistenceService.opprettValutakursgrunnlag(grunnlag)

        assertEquals(ValutakursgrunnlagStatus.OVERSTYRT, overstyrt.status)
        assertEquals(overstyrt.valutakursgrunnlagId, etterGjenforsøk.valutakursgrunnlagId)
        assertEquals(0, BigDecimal("11").compareTo(etterGjenforsøk.kurs))
        assertEquals(ValutakursgrunnlagKilde.MANUELL, etterGjenforsøk.kilde)
        assertEquals(null, etterGjenforsøk.observasjonsdato)
        assertNotNull(overstyrt.oppdatertTidspunkt)
    }

    @Test
    fun `ECB-krysskurs beholder presisjonen i kursgrunnlaget`() {
        val dato = LocalDate.of(2025, 7, 1)
        val lagret = persistenceService.opprettValutakursgrunnlag(
            ValutakursgrunnlagBo(
                brukFra = dato.atStartOfDay(),
                brukTil = dato.plusMonths(6).atStartOfDay(),
                kurs = BigDecimal("10.0574173757"),
                multiplikator = 0,
                basisvaluta = Valutakode.USD,
                kilde = ValutakursgrunnlagKilde.ECB,
                observasjonsdato = dato.minusMonths(1),
            ),
        )
        entityManager.clear()

        assertEquals(BigDecimal("10.0574173757000000"), repository.findById(lagret.valutakursgrunnlagId).orElseThrow().kurs)
    }

    @Test
    fun `feilet kurs kan lagres og overstyres for samme halvår`() {
        val dato = LocalDate.of(2025, 7, 1)
        val opprinnelig = repository.saveAndFlush(
            Valutakursgrunnlag(
                brukFra = dato.atStartOfDay(),
                brukTil = dato.plusMonths(6).atStartOfDay(),
                basisvaluta = Valutakode.DKK,
                feiletHenting = true,
            ),
        )
        entityManager.clear()
        val feilet = repository.hentValutakursgrunnlag(Valutakode.DKK, dato.atStartOfDay())
        assertNotNull(feilet)
        assertEquals(ValutakursgrunnlagStatus.FEILET, feilet?.status)

        repository.saveAndFlush(
            feilet!!.copy(kurs = BigDecimal("1.5534"), multiplikator = 0, feiletHenting = false).apply {
                status = ValutakursgrunnlagStatus.OVERSTYRT
            },
        )
        entityManager.clear()

        val overstyrt = repository.hentValutakursgrunnlag(Valutakode.DKK, dato.atStartOfDay())
        assertEquals(opprinnelig.valutakursgrunnlagId, overstyrt?.valutakursgrunnlagId)
        assertEquals(ValutakursgrunnlagStatus.OVERSTYRT, overstyrt?.status)
        assertEquals(BigDecimal("1.5534000000000000"), overstyrt?.kurs)
        assertNotNull(overstyrt?.oppdatertTidspunkt)
        assertEquals(null, repository.hentValutakursgrunnlag(Valutakode.DKK, dato.plusMonths(6).atStartOfDay()))
    }
}
