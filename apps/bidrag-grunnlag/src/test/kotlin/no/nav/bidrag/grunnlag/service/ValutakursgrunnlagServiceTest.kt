package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.bo.toValutakursgrunnlagEntity
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ValutakursgrunnlagServiceTest {
    private val persistence = Mockito.mock(PersistenceService::class.java)
    private val hent = Mockito.mock(HentValutakursService::class.java)
    private val service = ValutakursgrunnlagService(persistence, hent)
    private val dato = LocalDate.of(2025, 7, 1)

    @Test
    fun `feilet henting lagres for riktig valuta og halvår`() {
        Mockito.`when`(hent.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(dato, Valutakode.USD)))))
            .thenReturn(
                HentValutakursResponse(
                    listOf(
                        HentetValutakursResultat.FeiledValutakurs(
                            no.nav.bidrag.domene.tid.ÅrMånedsperiode("2025-06", "2025-07"),
                            Valutakode.USD,
                            Valutakode.NOK,
                        ),
                    ),
                ),
            )
        val repository = Mockito.mock(PersistenceService::class.java) { kall ->
            if (kall.method.name == "opprettValutakursgrunnlag") {
                val grunnlag = kall.getArgument<ValutakursgrunnlagBo>(0)
                grunnlag.toValutakursgrunnlagEntity()
            } else {
                null
            }
        }

        val resultat = ValutakursgrunnlagService(repository, hent).innhent(Valutakode.USD, dato)

        assertEquals(dato, resultat.brukFra.toLocalDate())
        assertEquals(dato.plusMonths(6), resultat.brukTil.toLocalDate())
        assertEquals(Valutakode.USD, resultat.basisvaluta)
        assertEquals(null, resultat.kurs)
        assertEquals(true, resultat.feiletHenting)
        assertEquals(null, resultat.kilde)
        assertEquals(null, resultat.observasjonsdato)
    }

    @Test
    fun `beløp fra fremmed valuta multipliseres med NOK-kursen og rundes til fire desimaler`() {
        lagreKurs(BigDecimal("10.12555"))

        val beregning = service.beregn(BigDecimal("2"), Valutakode.USD, Valutakode.NOK, dato)

        assertEquals(BigDecimal("20.2511"), beregning.beløp)
        assertEquals(BigDecimal("10.12555"), beregning.kurs)
        assertEquals(dato, beregning.kursgrunnlag?.brukFra?.toLocalDate())
    }

    @Test
    fun `beløp fra NOK divideres med NOK-kursen og rundes til fire desimaler`() {
        lagreKurs(BigDecimal("3"))

        assertEquals(BigDecimal("0.3333"), service.beregn(BigDecimal("1"), Valutakode.NOK, Valutakode.USD, dato).beløp)
    }

    @Test
    fun `NOK til NOK trenger ikke kursgrunnlag`() {
        assertEquals(BigDecimal("1.2346"), service.beregn(BigDecimal("1.23456"), Valutakode.NOK, Valutakode.NOK, dato).beløp)
        Mockito.verifyNoInteractions(persistence)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "1E+1000000000",
            "1E-1000000000",
            "-1E+1000000000",
            "0E+1000000000",
            "0E-1000000000",
            "1E+2147483647",
            "1E-2147483647",
            "1E+22",
            "10000000000000000000000",
            "0.00000000000000001",
            "1.00000000000000000",
            "9999999999999999999999.99999999999999999",
        ],
    )
    fun `for store beløp og eksponenter avvises før kursoppslag i alle beregningsveier`(verdi: String) {
        val beløp = BigDecimal(verdi)
        val beregninger = listOf<() -> Any>(
            { service.beregn(beløp, Valutakode.NOK, Valutakode.NOK, dato) },
            { service.beregn(beløp, Valutakode.NOK, Valutakode.USD, dato) },
            { service.beregn(beløp, Valutakode.USD, Valutakode.NOK, dato) },
            { service.fraNok(beløp, Valutakode.NOK, dato) },
            { service.fraNok(beløp, Valutakode.USD, dato) },
            { service.tilNok(beløp, Valutakode.NOK, dato) },
            { service.tilNok(beløp, Valutakode.USD, dato) },
        )

        beregninger.forEach { beregn ->
            val feil = assertThrows<ResponseStatusException> { beregn() }
            assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
            assertEquals("Beløp kan ha maksimalt 22 heltallssifre og 16 desimalplasser", feil.reason)
        }
        Mockito.verifyNoInteractions(persistence, hent)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "9999999999999999999999.9999999999999999",
            "-9999999999999999999999.9999999999999999",
            "1E+21",
            "1E-16",
            "0.0000000000000000",
            "-1.23456",
            "0",
        ],
    )
    fun `representerbare beløp inkludert grenseverdier beholder avrundingen`(verdi: String) {
        val beløp = BigDecimal(verdi)
        val forventet = beløp.setScale(4, java.math.RoundingMode.HALF_UP)
        lagreKurs(BigDecimal.ONE)

        assertEquals(forventet, service.beregn(beløp, Valutakode.NOK, Valutakode.NOK, dato).beløp)
        assertEquals(forventet, service.beregn(beløp, Valutakode.NOK, Valutakode.USD, dato).beløp)
        assertEquals(forventet, service.beregn(beløp, Valutakode.USD, Valutakode.NOK, dato).beløp)
        assertEquals(forventet, service.fraNok(beløp, Valutakode.NOK, dato))
        assertEquals(forventet, service.fraNok(beløp, Valutakode.USD, dato))
        assertEquals(forventet, service.tilNok(beløp, Valutakode.NOK, dato))
        assertEquals(forventet, service.tilNok(beløp, Valutakode.USD, dato))
    }

    @Test
    fun `aktiv følger halvårsgrensene ved oppslag overstyring og beregning`() {
        val fra = LocalDateTime.of(2026, 1, 1, 0, 0)
        val til = LocalDateTime.of(2026, 7, 1, 0, 0)
        val lagret = Valutakursgrunnlag(brukFra = fra, brukTil = til, basisvaluta = Valutakode.USD, kurs = BigDecimal.TEN, multiplikator = 0)
        val oppslagsdato = fra.toLocalDate()
        Mockito.`when`(persistence.hentValutakursgrunnlag(Valutakode.USD, oppslagsdato)).thenReturn(lagret)
        Mockito.`when`(persistence.overstyrValutakursgrunnlag(42, BigDecimal.TEN)).thenReturn(lagret)
        val tidspunkt = listOf(fra.minusNanos(1) to false, fra to true, til.minusNanos(1) to true, til to false, til.plusMonths(1) to false)

        tidspunkt.forEach { (nå, forventet) ->
            Mockito.mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS).use { tid ->
                tid.`when`<LocalDateTime> { LocalDateTime.now() }.thenReturn(nå)

                assertEquals(forventet, service.hentValutakursgrunnlag(Valutakode.USD, oppslagsdato)?.aktiv)
                assertEquals(forventet, service.overstyr(42, BigDecimal.TEN).aktiv)
                assertEquals(forventet, service.beregn(BigDecimal.ONE, Valutakode.USD, Valutakode.NOK, oppslagsdato).kursgrunnlag?.aktiv)
            }
        }
    }

    @Test
    fun `liste over feilede grunnlag beregner aktiv ved lesing`() {
        val nå = LocalDateTime.of(2026, 7, 1, 0, 0)
        val utløpt = Valutakursgrunnlag(brukFra = nå.minusMonths(6), brukTil = nå, basisvaluta = Valutakode.USD, feiletHenting = true)
        val gjeldende = Valutakursgrunnlag(brukFra = nå, brukTil = nå.plusMonths(6), basisvaluta = Valutakode.EUR, feiletHenting = true)
        val pageable = PageRequest.of(0, 50)
        Mockito.`when`(persistence.hentFeiledeValutakursgrunnlag(pageable)).thenReturn(PageImpl(listOf(utløpt, gjeldende)))

        Mockito.mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS).use { tid ->
            tid.`when`<LocalDateTime> { LocalDateTime.now() }.thenReturn(nå)

            assertEquals(listOf(false, true), service.hentFeiledeValutakursgrunnlag(pageable).content.map { it.aktiv })
        }
    }

    @Test
    fun `feilet kursgrunnlag kan ikke brukes i kalkulatoren`() {
        Mockito.`when`(persistence.hentValutakursgrunnlag(Valutakode.USD, dato)).thenReturn(
            Valutakursgrunnlag(brukFra = dato.atStartOfDay(), brukTil = dato.plusMonths(6).atStartOfDay(), basisvaluta = Valutakode.USD, feiletHenting = true),
        )

        val feil = assertThrows<ResponseStatusException> { service.beregn(BigDecimal.ONE, Valutakode.USD, Valutakode.NOK, dato) }

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, feil.statusCode)
    }

    @Test
    fun `manglende kurs og krysskurs gir eksplisitte feil`() {
        assertEquals(
            HttpStatus.NOT_FOUND,
            assertThrows<ResponseStatusException> { service.beregn(BigDecimal.ONE, Valutakode.USD, Valutakode.NOK, dato) }.statusCode,
        )
        assertEquals(
            HttpStatus.BAD_REQUEST,
            assertThrows<ResponseStatusException> { service.beregn(BigDecimal.ONE, Valutakode.USD, Valutakode.EUR, dato) }.statusCode,
        )
    }

    @Test
    fun `samtidig innsetting forsøkes på nytt mot den lagrede raden`() {
        val eksisterende = Valutakursgrunnlag(
            valutakursgrunnlagId = 42,
            brukFra = dato.atStartOfDay(),
            brukTil = dato.plusMonths(6).atStartOfDay(),
            basisvaluta = Valutakode.USD,
            kurs = BigDecimal("10"),
            multiplikator = 0,
        )
        var forsøk = 0
        var lagringsgrunnlag: ValutakursgrunnlagBo? = null
        val repository = Mockito.mock(PersistenceService::class.java) { kall ->
            if (kall.method.name == "opprettValutakursgrunnlag") {
                lagringsgrunnlag = kall.getArgument(0)
                if (++forsøk == 1) throw DataIntegrityViolationException("Unik nøkkel")
                eksisterende
            } else {
                null
            }
        }

        val resultat = ValutakursgrunnlagService(repository, hent).opprettValutakursgrunnlag(
            listOf(
                HentetValutakursResultat.HentetValutakurs(
                    no.nav.bidrag.domene.tid.ÅrMånedsperiode("2025-06", "2025-07"),
                    BigDecimal("10"),
                    0,
                    Valutakode.USD,
                    Valutakode.NOK,
                    dato.atStartOfDay(),
                    ValutakursgrunnlagKilde.ECB,
                ),
            ),
            Datoperiode(dato, dato.plusMonths(6)),
        )

        assertEquals(42, resultat.single().valutakursgrunnlagId)
        assertEquals(2, forsøk)
        assertEquals(ValutakursgrunnlagKilde.ECB, lagringsgrunnlag?.kilde)
        assertEquals(LocalDate.of(2025, 6, 1), lagringsgrunnlag?.observasjonsdato)
    }

    private fun lagreKurs(kurs: BigDecimal) {
        Mockito.`when`(persistence.hentValutakursgrunnlag(Valutakode.USD, dato)).thenReturn(
            Valutakursgrunnlag(brukFra = dato.atStartOfDay(), brukTil = dato.plusMonths(6).atStartOfDay(), basisvaluta = Valutakode.USD, kurs = kurs, multiplikator = 0),
        )
    }
}
