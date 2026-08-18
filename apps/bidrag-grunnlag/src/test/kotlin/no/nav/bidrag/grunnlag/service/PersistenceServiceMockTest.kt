package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.inntekt.Skattegrunnlagstype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import no.nav.bidrag.grunnlag.bo.toSkattegrunnlagEntity
import no.nav.bidrag.grunnlag.bo.toSkattegrunnlagspostEntity
import no.nav.bidrag.grunnlag.comparator.PeriodComparable
import no.nav.bidrag.grunnlag.persistence.entity.Skattegrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.Skattegrunnlagspost
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PersistenceServiceMockTest {

    @InjectMocks
    private lateinit var persistenceService: PersistenceService

    @Mock
    private lateinit var grunnlagspakkeRepositoryMock: GrunnlagspakkeRepository

    @Mock
    private lateinit var ainntektRepositoryMock: AinntektRepository

    @Mock
    private lateinit var ainntektspostRepositoryMock: AinntektspostRepository

    @Mock
    private lateinit var skattegrunnlagRepositoryMock: SkattegrunnlagRepository

    @Mock
    private lateinit var skattegrunnlagspostRepositoryMock: SkattegrunnlagspostRepository

    @Mock
    private lateinit var utvidetBarnetrygdOgSmaabarnstilleggRepositoryMock: UtvidetBarnetrygdOgSmaabarnstilleggRepository

    @Mock
    private lateinit var barnetilleggRepositoryMock: BarnetilleggRepository

    @Mock
    private lateinit var relatertPersonRepositoryMock: RelatertPersonRepository

    @Mock
    private lateinit var sivilstandRepositoryMock: SivilstandRepository

    @Mock
    private lateinit var kontantstotteRepositoryMock: KontantstotteRepository

    @Mock
    private lateinit var barnetilsynRepositoryMock: BarnetilsynRepository

    @Test
    fun `Skal hente skattegrunnlag for 1 personid som finnes i lista`() {
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1)).thenReturn(TestUtil.byggSkattegrunnlagRepositoryListeForEnIdent())
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1)).thenReturn(TestUtil.byggSkattegrunnlagspostRepositoryListe())

        val personIdListe = listOf("12345678901", "12345678902")
        val skattegrunnlagForPersonIdListe = persistenceService.hentSkattegrunnlagForPersonIdToCompare(
            grunnlagspakkeId = 1,
            personIdListe = personIdListe,
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).hentSkattegrunnlagsposter(1)

        assertAll(
            { assertThat(skattegrunnlagForPersonIdListe).isNotNull() },
        )
    }

    @Test
    fun `Skal hente skattegrunnlag for 2 personid-er som finnes i lista`() {
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1)).thenReturn(TestUtil.byggSkattegrunnlagRepositoryListeForToIdenter())
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1)).thenReturn(TestUtil.byggSkattegrunnlagspostRepositoryListe())

        val personIdListe = listOf("12345678901", "12345678902")
        val skattegrunnlagForPersonIdListe = persistenceService.hentSkattegrunnlagForPersonIdToCompare(
            grunnlagspakkeId = 1,
            personIdListe = personIdListe,
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(2)).hentSkattegrunnlagsposter(1)

        assertAll(
            { assertThat(skattegrunnlagForPersonIdListe).isNotNull() },
        )
    }

    @Test
    fun `Skal ikke hente skattegrunnlag hvis ingen personer i lista matcher`() {
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1)).thenReturn(TestUtil.byggSkattegrunnlagRepositoryListeForToIdenter())

        val personIdListe = listOf("12345678903", "12345678904")
        val skattegrunnlagForPersonIdListe = persistenceService.hentSkattegrunnlagForPersonIdToCompare(
            grunnlagspakkeId = 1,
            personIdListe = personIdListe,
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.never()).hentSkattegrunnlagsposter(1)

        assertAll(
            { assertThat(skattegrunnlagForPersonIdListe).isNotNull() },
        )
    }

    @Test
    fun `Skal lagre nye skattegrunnlag når det ikke finnes noen fra før`() {
        // Eksisterende forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1)).thenReturn(emptyList())

        // Nye/oppdaterte forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlag())
        Mockito.`when`(skattegrunnlagspostRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlagspost())

        // Personid-er i requesten (inklusiv historiske id-er)
        val personIdListeRequest = listOf("12345678901", "12345678902")

        // Nye skattegrunnlag hentet via Sigrun-consumer
        val nyeSkattegrunnlag = listOf(PeriodComparable(byggSkattegrunnlagBo("12345678901"), listOf(byggSkattegrunnlagspostBo())))

        // Kall til PersistenceService
        persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
            grunnlagspakkeId = 1,
            newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
            periodeFra = LocalDate.parse("2023-01-01"),
            periodeTil = LocalDate.now(),
            personIdListe = personIdListeRequest,
            timestampOppdatering = LocalDateTime.now(),
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(0)).hentSkattegrunnlagsposter(1)

        // updatedEntities = 1
        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).save(nyeSkattegrunnlag[0].periodEntity.toSkattegrunnlagEntity())
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).save(MockitoHelper.any(Skattegrunnlagspost::class.java))
    }

    @Test
    fun `Skattegrunnlag, forekomster finnes fra før, men de er ikke endret, skal oppdatere tidspunkt`() {
        // Eksisterende forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1))
            .thenReturn(listOf(byggSkattegrunnlagBo("12345678901").toSkattegrunnlagEntity()))
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1))
            .thenReturn(listOf(byggSkattegrunnlagspostBo().toSkattegrunnlagspostEntity()))

        // Nye/oppdaterte forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlag())

        // Personid-er i requesten (inklusiv historiske id-er)
        val personIdListeRequest = listOf("12345678901", "12345678902")

        // Nye skattegrunnlag hentet via Sigrun-consumer
        val nyeSkattegrunnlag = listOf(PeriodComparable(byggSkattegrunnlagBo("12345678901"), listOf(byggSkattegrunnlagspostBo())))

        // Kall til PersistenceService
        persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
            grunnlagspakkeId = 1,
            newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
            periodeFra = LocalDate.parse("2021-01-01"),
            periodeTil = LocalDate.parse("2022-01-01"),
            personIdListe = personIdListeRequest,
            timestampOppdatering = LocalDateTime.now(),
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).hentSkattegrunnlagsposter(1)

        // equalEntities = 1
        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).save(MockitoHelper.any(Skattegrunnlag::class.java))
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(0)).save(MockitoHelper.any(Skattegrunnlagspost::class.java))
    }

    @Test
    fun `Skattegrunnlag, forekomster finnes fra før, og de er endret, setter eksisterende til inaktive og oppretter nye`() {
        // Eksisterende forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1))
            .thenReturn(listOf(byggSkattegrunnlagBo("12345678901").toSkattegrunnlagEntity()))
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1))
            .thenReturn(listOf(byggSkattegrunnlagspostBo().toSkattegrunnlagspostEntity()))

        // Nye/oppdaterte forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlag())
        Mockito.`when`(skattegrunnlagspostRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlagspost())

        // Personid-er i requesten (inklusiv historiske id-er)
        val personIdListeRequest = listOf("12345678901", "12345678902")

        // Nye skattegrunnlag hentet via Sigrun-consumer
        val nyeSkattegrunnlag = listOf(PeriodComparable(byggSkattegrunnlagBo("12345678901"), listOf(byggSkattegrunnlagspostBoEndret())))

        // Kall til PersistenceService
        persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
            grunnlagspakkeId = 1,
            newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
            periodeFra = LocalDate.parse("2021-01-01"),
            periodeTil = LocalDate.parse("2022-01-01"),
            personIdListe = personIdListeRequest,
            timestampOppdatering = LocalDateTime.now(),
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).hentSkattegrunnlagsposter(1)

        // expiredEntities = 1; updatedEntities = 1
        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(2)).save(MockitoHelper.any(Skattegrunnlag::class.java))
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).save(MockitoHelper.any(Skattegrunnlagspost::class.java))
    }

    @Test
    fun `Skattegrunnlag, forekomster finnes fra før for en historisk ident, men er ikke endret, skal oppdatere tidspunkt`() {
        // Eksisterende forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1))
            .thenReturn(listOf(byggSkattegrunnlagBo("12345678902").toSkattegrunnlagEntity()))
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1))
            .thenReturn(listOf(byggSkattegrunnlagspostBo().toSkattegrunnlagspostEntity()))

        // Nye/oppdaterte forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlag())

        // Personid-er i requesten (inklusiv historiske id-er)
        val personIdListeRequest = listOf("12345678901", "12345678902")

        // Nye skattegrunnlag hentet via Sigrun-consumer
        val nyeSkattegrunnlag = listOf(PeriodComparable(byggSkattegrunnlagBo("12345678901"), listOf(byggSkattegrunnlagspostBo())))

        // Kall til PersistenceService
        persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
            grunnlagspakkeId = 1,
            newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
            periodeFra = LocalDate.parse("2021-01-01"),
            periodeTil = LocalDate.parse("2022-01-01"),
            personIdListe = personIdListeRequest,
            timestampOppdatering = LocalDateTime.now(),
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).hentSkattegrunnlagsposter(1)

        // equalEntities = 1
        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).save(MockitoHelper.any(Skattegrunnlag::class.java))
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(0)).save(MockitoHelper.any(Skattegrunnlagspost::class.java))
    }

    @Test
    fun `Skattegrunnlag, forekomster finnes fra før for en historisk ident, og den er endret, setter eksisterende til inaktive og oppretter nye`() {
        // Eksisterende forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.hentSkattegrunnlag(1))
            .thenReturn(listOf(byggSkattegrunnlagBo("12345678902").toSkattegrunnlagEntity()))
        Mockito.`when`(skattegrunnlagspostRepositoryMock.hentSkattegrunnlagsposter(1))
            .thenReturn(listOf(byggSkattegrunnlagspostBo().toSkattegrunnlagspostEntity()))

        // Nye/oppdaterte forekomster
        Mockito.`when`(skattegrunnlagRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlag())
        Mockito.`when`(skattegrunnlagspostRepositoryMock.save(MockitoHelper.any())).thenReturn(Skattegrunnlagspost())

        // Personid-er i requesten (inklusiv historiske id-er)
        val personIdListeRequest = listOf("12345678901", "12345678902")

        // Nye skattegrunnlag hentet via Sigrun-consumer
        val nyeSkattegrunnlag = listOf(PeriodComparable(byggSkattegrunnlagBo("12345678901"), listOf(byggSkattegrunnlagspostBoEndret())))

        // Kall til PersistenceService
        persistenceService.oppdaterSkattegrunnlagForGrunnlagspakke(
            grunnlagspakkeId = 1,
            newSkattegrunnlagForPersonId = nyeSkattegrunnlag,
            periodeFra = LocalDate.parse("2021-01-01"),
            periodeTil = LocalDate.parse("2022-01-01"),
            personIdListe = personIdListeRequest,
            timestampOppdatering = LocalDateTime.now(),
        )

        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(1)).hentSkattegrunnlag(1)
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).hentSkattegrunnlagsposter(1)

        // expiredEntities = 1; updatedEntities = 1
        Mockito.verify(skattegrunnlagRepositoryMock, Mockito.times(2)).save(MockitoHelper.any(Skattegrunnlag::class.java))
        Mockito.verify(skattegrunnlagspostRepositoryMock, Mockito.times(1)).save(MockitoHelper.any(Skattegrunnlagspost::class.java))
    }

    private fun byggSkattegrunnlagBo(personId: String) = SkattegrunnlagBo(
        skattegrunnlagId = 1,
        grunnlagspakkeId = 1,
        personId = personId,
        periodeFra = LocalDate.parse("2021-01-01"),
        periodeTil = LocalDate.parse("2022-01-01"),
        aktiv = true,
        brukFra = LocalDateTime.now(),
        brukTil = null,
        hentetTidspunkt = LocalDateTime.now(),
    )

    private fun byggSkattegrunnlagspostBo() = SkattegrunnlagspostBo(
        skattegrunnlagId = 1,
        skattegrunnlagType = Skattegrunnlagstype.ORDINÆR.toString(),
        inntektType = Inntektstype.LØNNSINNTEKT.toString(),
        belop = BigDecimal.valueOf(171717),
    )

    private fun byggSkattegrunnlagspostBoEndret() = SkattegrunnlagspostBo(
        skattegrunnlagId = 1,
        skattegrunnlagType = Skattegrunnlagstype.ORDINÆR.toString(),
        inntektType = Inntektstype.LØNNSINNTEKT.toString(),
        belop = BigDecimal.valueOf(181818),
    )

    object MockitoHelper {
        fun <T> any(type: Class<T>): T = Mockito.any(type)
        fun <T> any(): T = Mockito.any()
    }
}
