package no.nav.bidrag.grunnlag.persistence

import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.BidragGrunnlag
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagStatus
import no.nav.bidrag.grunnlag.persistence.entity.toValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.persistence.repository.ValutakursgrunnlagRepository
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.service.PersistenceService
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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
    fun `batchoppslag returnerer kun grunnlag aktivt på datoen`() {
        val januar = LocalDate.of(2024, 1, 1)
        val juli = LocalDate.of(2024, 7, 1)
        val januarNeste = LocalDate.of(2025, 1, 1)
        repository.saveAllAndFlush(
            listOf(
                Valutakursgrunnlag(
                    brukFra = januar.atStartOfDay(),
                    brukTil = juli.atStartOfDay(),
                    basisvaluta = Valutakode.USD,
                    kurs = BigDecimal.TEN,
                    multiplikator = 0,
                ),
                Valutakursgrunnlag(
                    brukFra = juli.atStartOfDay(),
                    brukTil = januarNeste.atStartOfDay(),
                    basisvaluta = Valutakode.USD,
                    kurs = BigDecimal.TEN,
                    multiplikator = 0,
                ),
                Valutakursgrunnlag(
                    brukFra = juli.atStartOfDay(),
                    brukTil = januarNeste.atStartOfDay(),
                    basisvaluta = Valutakode.DKK,
                    kurs = null,
                    multiplikator = null,
                    feiletHenting = true,
                ),
            ),
        )
        entityManager.clear()

        val grunnlagJanuar = repository.hentValutakursgrunnlagForDato(januar.atStartOfDay())
        val grunnlagJuli = repository.hentValutakursgrunnlagForDato(juli.atStartOfDay())

        grunnlagJanuar.map { it.basisvaluta }.toSet() shouldBe setOf(Valutakode.USD)
        grunnlagJanuar.single().brukFra shouldBe januar.atStartOfDay()
        grunnlagJuli.map { it.basisvaluta }.toSet() shouldBe setOf(Valutakode.USD, Valutakode.DKK)
        grunnlagJuli.all { it.brukFra == juli.atStartOfDay() } shouldBe true
        grunnlagJuli.single { it.basisvaluta == Valutakode.DKK }.feiletHenting shouldBe true
    }

    @Test
    fun `mislykket gjeninnhenting bevarer HENTET-kurs og tillater fortsatt omregning`() {
        val dato = LocalDate.of(2025, 7, 1)
        val lagret = persistenceService.opprettValutakursgrunnlag(
            ValutakursgrunnlagBo(
                brukFra = dato.atStartOfDay(),
                brukTil = dato.plusMonths(6).atStartOfDay(),
                kurs = BigDecimal("10.125"),
                multiplikator = 0,
                basisvaluta = Valutakode.USD,
                kilde = ValutakursgrunnlagKilde.ECB,
                observasjonsdato = dato.minusMonths(1),
            ),
        ).toValutakursgrunnlagBo()
        val hent = Mockito.mock(HentValutakursService::class.java)
        Mockito.`when`(hent.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(dato, Valutakode.USD)))))
            .thenReturn(
                HentValutakursResponse(
                    listOf(HentetValutakursResultat.FeiledValutakurs(ÅrMånedsperiode("2025-06", "2025-07"), Valutakode.USD, Valutakode.NOK)),
                ),
            )
        val service = ValutakursgrunnlagService(persistenceService, hent)
        entityManager.clear()

        assertEquals(lagret, service.innhent(Valutakode.USD, dato))
        entityManager.clear()

        assertEquals(lagret, service.hentValutakursgrunnlag(Valutakode.USD, dato))
        assertEquals(1L, repository.count())
        val beregning = service.beregn(BigDecimal("100"), Valutakode.USD, Valutakode.NOK, dato)
        assertEquals(BigDecimal("1012.5000"), beregning.beløp)
        assertEquals(lagret, beregning.kursgrunnlag)
    }

    @Test
    fun `aktiv beregnes for lagret grunnlag uten egen databasekolonne`() {
        val lagret = repository.saveAndFlush(
            Valutakursgrunnlag(
                brukFra = LocalDate.of(2020, 1, 1).atStartOfDay(),
                brukTil = LocalDate.of(2020, 7, 1).atStartOfDay(),
                basisvaluta = Valutakode.USD,
                kurs = BigDecimal("10.125"),
                multiplikator = 0,
            ),
        )
        entityManager.clear()

        val lest = repository.findById(lagret.valutakursgrunnlagId).orElseThrow()
        val antallAktivKolonner = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'valutakursgrunnlag' AND LOWER(column_name) = 'aktiv'",
        ).singleResult as Number

        assertEquals(0L, antallAktivKolonner.toLong())
        assertEquals(lagret.brukFra, lest.brukFra)
        assertEquals(lagret.brukTil, lest.brukTil)
        assertEquals(lagret.basisvaluta, lest.basisvaluta)
        assertEquals(0, lagret.kurs!!.compareTo(lest.kurs))
        assertEquals(lagret.multiplikator, lest.multiplikator)
        assertEquals(false, lest.aktiv)
        assertEquals(false, lest.toValutakursgrunnlagBo().aktiv)
    }

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
