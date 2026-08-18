package no.nav.bidrag.grunnlag.model

import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension::class)
class OppdaterBarnetilsynTest {

    @ParameterizedTest
    @MethodSource("barnMedForskjelligeAldereForBeregningAvSkolealder")
    fun skalBeregneSkolealder(barnIdent: String, fom: LocalDate, skolealder: Skolealder) {
        val mockLocalDate = LocalDate.of(2024, 1, 1)

        // Mock the LocalDate.now() method
        Mockito.mockStatic(LocalDate::class.java).use { mockedStatic ->
            mockedStatic.`when`<LocalDate> { LocalDate.now() }.thenReturn(mockLocalDate)
        }
    }

    private fun barnMedForskjelligeAldereForBeregningAvSkolealder(): Stream<Arguments> = Stream.of(
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31)), LocalDate.of(2022, 1, 1), Skolealder.UNDER),
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31).minusYears(4)), LocalDate.of(2016, 1, 1), Skolealder.UNDER),
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31).minusYears(6)), LocalDate.of(2022, 8, 1), Skolealder.OVER),
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31).minusYears(5)), LocalDate.of(2022, 8, 1), Skolealder.OVER),
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31).minusYears(6)), LocalDate.of(2022, 1, 1), Skolealder.UNDER),
        Arguments.of(opprettBarnIdentFraDato(LocalDate.of(2023, 12, 31).minusYears(12)), LocalDate.of(2022, 1, 1), Skolealder.OVER),
    )

    private fun opprettBarnIdentFraDato(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("ddMMyy")) + "45364"
}
